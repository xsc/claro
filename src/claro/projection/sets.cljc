(ns claro.projection.sets
  (:require [claro.projection.protocols :refer [Projection project-template]]
            [claro.data.ops
             [collections :as c]
             [then :refer [then]]]))

;; ## Helpers

(defn- assert-set!
  [value]
  (when-not (and (coll? value) (not (map? value)))
    (throw
      (IllegalArgumentException.
        (str "projection template is set but value is not a collection: "
             (pr-str value)))))
  value)

(defn- project-set
  [template value]
  (assert-set! value)
  (-> (c/map-single #(project-template template %) value)
      (then set)))

;; ## Implementation

(extend-protocol Projection
  clojure.lang.IPersistentSet
  (project-template [sq value]
    {:pre [(= (count sq) 1)]}
    (let [template (first sq)]
      (then value #(project-set template %)))))