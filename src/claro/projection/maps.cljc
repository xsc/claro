(ns claro.projection.maps
  (:refer-clojure :exclude [alias])
  (:require [claro.projection.protocols :as pr]
            [claro.data.ops.chain :as chain]
            [claro.data.error :refer [with-error?]]
            [potemkin :refer [defprotocol+]]))

;; ## Helpers

(defn- throw-missing-key!
  [value-projection k]
  (throw
    (IllegalArgumentException.
      (format
        (str "projection template expects key '%s' but value does not "
             "contain it.%n"
             "value template: %s")
        (pr-str k)
        (pr-str value-projection)))))

(defn- rethrow-with-meta!
  [^Throwable t projection unresolved-value value]
  (throw
    (IllegalArgumentException.
      (format
        (str "%s%n"
             "resolvable:     %s%n"
             "template:       %s%n"
             "available keys: %s")
        (.getMessage t)
        (pr-str unresolved-value)
        (pr-str projection)
        (pr-str (vec (keys value)))))))

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

(defn- assert-no-override!
  [alias-projection value k]
  (when (contains? value k)
    (throw
      (IllegalArgumentException.
        (format "'alias' projection '%s' would override key '%s'."
                (pr-str alias-projection)
                (pr-str k))))))

;; ## Value Projections
;;
;; This can be used by `maybe` and `default` projections to provide
;; default values, as well as by `value` to override existing values.

(defprotocol+ MapValueProjection
  "Protocol for projections used for map values."
  (project-value
    [value-projection value]
    "Project the given map value.")
  (project-missing-value
    [value-projection k]
    "Handle a missing map value at the given key."))

(extend-protocol MapValueProjection
  Object
  (project-value [value-projection value]
    (pr/project value-projection value))
  (project-missing-value [value-projection k]
    (throw-missing-key! value-projection k))

  nil
  (project-value [value-projection value]
    (pr/project value-projection value))
  (project-missing-value [value-projection k]
    (throw-missing-key! value-projection k)))

(defn- project-value-or-missing
  [value-projection m k]
  (let [value (get m k ::none)]
    (if (= value ::none)
      (project-missing-value value-projection k)
      (project-value value-projection value))))

;; ## Map Key Handling

(defprotocol+ MapKeyProjection
  "Protocol for map key projection semantics, e.g. aliasing."
  (target-key [key-projection value])
  (source-key [key-projection value]))

(extend-protocol MapKeyProjection
  Object
  (target-key [this value] this)
  (source-key [this value] this)

  nil
  (target-key [this value] this)
  (source-key [this value] this))

;; ## Assertions

;; ## Alias Keys
;;
;; This can be used directly in maps to indicate a renamed/copied key.

(deftype AliasKey [alias-key key]
  MapKeyProjection
  (target-key [this value]
    (assert-no-override! this value alias-key)
    alias-key)
  (source-key [_ value]
    key))

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
      (try
        (loop [template (seq this)
               keys     (transient [])
               values   (transient [])]
          (if template
            (let [e (first template)
                  key-projection (key e)
                  value-projection (val e)
                  tk (target-key key-projection value)
                  sk (source-key key-projection value)
                  v  (project-value-or-missing value-projection value sk)]
              (recur
                (next template)
                (conj! keys tk)
                (conj! values v)))
            (let [ks (persistent! keys)
                  vs (persistent! values)]
              (chain/chain-blocking* vs #(zipmap ks %)))))
        (catch Throwable t
          (rethrow-with-meta! t this unresolved-value value)))
      {})))

(extend-protocol pr/Projection
  clojure.lang.IPersistentMap
  (pr/project [this value]
    (chain/chain-eager
      value
      #(project-keys this value %))))
