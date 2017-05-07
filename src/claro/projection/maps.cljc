(ns claro.projection.maps
  (:refer-clojure :exclude [alias])
  (:require [claro.projection.protocols :as pr]
            [claro.projection.value :refer [value?]]
            [claro.data.ops.chain :as chain]
            [claro.data.error :refer [with-error?]]))

;; ## Assertions

(defn- assert-map!
  [unresolved-value value template]
  (when-not (map? value)
    (throw
      (IllegalArgumentException.
        (format
          (str "projection template is a map but value is not.%n"
               "resolvable: %s%n"
               "template:   %s%n"
               "value:      %s")
          (pr-str unresolved-value)
          (pr-str template)
          (pr-str value)))))
  value)

(defn- assert-contains!
  [this unresolved-value value k template]
  (when-not (or (contains? value k)
                (value? template))
    (throw
      (IllegalArgumentException.
        (format
          (str "projection template expects key '%s' but value does not "
               "contain it.%n"
               "resolvable:     %s%n"
               "template:       %s%n"
               "available keys: %s")
          k
          (pr-str unresolved-value)
          (pr-str this)
          (pr-str (vec (keys value)))))))
  value)

(defn- assert-no-override!
  [this unresolved-value value k]
  (when (contains? value k)
    (throw
      (IllegalArgumentException.
        (format
          (str "'alias' projection " (pr-str this)
               " would override key '" k "'.%n"
               "resolvable: %s%n"
               "template:   %s%n"
               "value:      %s")
          (pr-str unresolved-value)
          (pr-str this)
          (pr-str value)))))
  value)

;; ## Projection Logic

(defn- project-value
  [this unresolved-value value key template]
  (assert-contains! this unresolved-value value key template)
  (pr/project template (get value key)))

(defn- project-aliased-value
  [this unresolved-value value alias-key key template]
  (assert-no-override! this unresolved-value value alias-key)
  (project-value this unresolved-value value key template))

;; ## Alias Keys
;;
;; This can be used directly in maps to indicate a renamed/copied key.

(defrecord AliasKey [alias-key key])

(defmethod print-method AliasKey
  [^AliasKey value ^java.io.Writer w]
  (.write w "#<claro/alias ")
  (print-method (.-key value) w)
  (.write w " => ")
  (print-method (.-alias-key value) w)
  (.write w ">"))

(defn alias
  "This function can be used within maps to rename/copy an existing key
   for further projection, e.g.:

   ```clojure
   {:id                          projection/leaf
    (alias :friend-id :friend)   {:id projection/leaf}
    (alias :friend-name :friend) {:name projection/leaf}}
   ```

   This would result in a map of the following shape:

   ```clojure
   {:id          1
    :friend-id   {:id 2}
    :friend-name {:name \"Dr. Watson\"}}
   ```
   "
  [alias-key key]
  {:pre [(not= alias-key key)]}
  (->AliasKey alias-key key))

;; ## Default Map Implementation

(defn- project-keys
  [this unresolved-value value]
  (with-error? value
    (assert-map! unresolved-value value this)
    (if-not (empty? this)
      (let [project-unaliased
            #(project-value this unresolved-value value %1 %2)
            project-aliased
            #(project-aliased-value this unresolved-value value %1 %2 %3)]
        (loop [template (seq this)
               keys     (transient [])
               values   (transient [])]
          (if template
            (let [[k value-template] (first template)]
              (if (instance? AliasKey k)
                (let [{:keys [key alias-key]} k
                      value (project-aliased alias-key key value-template)]
                  (recur
                    (next template)
                    (conj! keys alias-key)
                    (conj! values value)))
                (let [value (project-unaliased k value-template)]
                  (recur
                    (next template)
                    (conj! keys k)
                    (conj! values value)))))
            (let [ks (persistent! keys)
                  vs (persistent! values)]
              (chain/chain-blocking* vs #(zipmap ks %))))))
      {})))

(extend-protocol pr/Projection
  clojure.lang.IPersistentMap
  (pr/project [this value]
    (chain/chain-eager
      value
      #(project-keys this value %))))
