(ns claro.projection.maps
  (:require [claro.projection.protocols :as pr]
            [claro.data.ops
             [then :refer [then]]
             [maps :as m]]))

;; ## Helpers

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
  [value template k]
  (when-not (contains? value k)
    (throw
      (IllegalArgumentException.
        (format
          (str "projection template expects key '%s' but value does not "
               "contain it.%n"
               "template:       %s%n"
               "available keys: %s")
          k
          (pr-str template)
          (pr-str (vec (keys value)))))))
  value)

(defn- project-keys
  [value templates]
  (reduce
    (fn [value [k template]]
      (-> value
          (assert-contains! templates k)
          (update k #(pr/project template %))))
    value templates))

;; ## Implementation

(extend-protocol pr/Projection
  clojure.lang.IPersistentMap
  (pr/project [templates value]
    (let [ks (keys templates)]
      (then
        value
        (comp #(select-keys % ks)
              #(project-keys % templates)
              #(assert-map! % templates))))))
