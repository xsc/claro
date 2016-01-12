(ns claro.data.projection.maps
  (:require [claro.data.protocols :refer [Projection project-template]]
            [claro.data.ops
             [then :refer [then]]
             [maps :as m]]))

;; ## Helpers

(defn- assert-map!
  [value]
  (when-not (map? value)
    (throw
      (IllegalArgumentException.
        (str "projection template is a map but value is not: "
             (pr-str value)))))
  value)

(defn- assert-contains!
  [value k]
  (when-not (contains? value k)
    (throw
      (IllegalArgumentException.
        (str "projection template expects key '" k "' but value "
             "does not contain it: " (pr-str value)))))
  value)

(defn- project-keys
  [value templates]
  (reduce
    (fn [value [k template]]
      (-> value
          (assert-contains! k)
          (update k then #(project-template template %))))
    value templates))

;; ## Implementation

(extend-protocol Projection
  clojure.lang.IPersistentMap
  (project-template [templates value]
    (let [ks (keys templates)]
      (then
        value
        (comp #(select-keys % ks)
              #(project-keys % templates)
              assert-map!)))))
