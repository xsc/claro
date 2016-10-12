(ns claro.projection.maps
  (:refer-clojure :exclude [alias])
  (:require [claro.projection.protocols :as pr]
            [claro.projection.value :refer [value?]]
            [claro.data.ops.then :refer [then]]
            [claro.data.error :refer [with-error?]]))

;; ## Assertions

(defn- assert-map!
  [value template]
  (when-not (map? value)
    (throw
      (IllegalArgumentException.
        (format
          (str "projection template is a map but value is not.%n"
               "template: %s%n"
               "value:    %s")
                (pr-str template)
                (pr-str value)))))
  value)

(defn- assert-contains!
  [value k template this]
  (when-not (or (contains? value k)
                (value? template))
    (throw
      (IllegalArgumentException.
        (format
          (str "projection template expects key '%s' but value does not "
               "contain it.%n"
               "template:       %s%n"
               "available keys: %s")
          k
          (pr-str this)
          (pr-str (vec (keys value)))))))
  value)

(defn- assert-no-override!
  [value k this]
  (when (contains? value k)
    (throw
      (IllegalArgumentException.
        (str "'alias' projection " (pr-str this)
             " would override key '" k "' in: "
             (pr-str value)))))
  value)

;; ## Projection Logic

(defn- project-value
  [value key template this]
  (-> value
      (assert-contains! key template this)
      (get key)
      (->> (pr/project template))))

(defn- project-aliased-value
  [value alias-key key template this]
  (-> value
      (assert-no-override! alias-key this)
      (project-value key template this)))

;; ## Alias Keys
;;
;; This can be used directly in maps to indicate a renamed/copied key.

(defrecord AliasKey [alias-key key])

(defmethod print-method AliasKey
  [^AliasKey value ^java.io.Writer w]
  (.write w "#<alias ")
  (print-method (.-key value) w)
  (.write w " -> ")
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
  [this value]
  (with-error? value
    (assert-map! value this)
    (reduce
      (fn [result [k template]]
        (if (instance? AliasKey k)
          (let [{:keys [key alias-key]} k]
            (->> (project-aliased-value value alias-key key template this)
                 (assoc result alias-key)))
          (->> (project-value value k template this)
               (assoc result k))))
      {} this)))

(extend-protocol pr/Projection
  clojure.lang.IPersistentMap
  (pr/project [this value]
    (then value #(project-keys this %))))
