(ns claro.projection.sets
  (:require [claro.projection.protocols :refer [Projection project]]
            [claro.data.ops
             [collections :as c]
             [then :refer [then]]]))

;; ## Helpers

(defn- assert-set!
  [value template]
  (when-not (and (coll? value) (not (map? value)))
    (throw
      (IllegalArgumentException.
        (str "projection template is set but value is not.\n"
             "template: #{" (pr-str template) "}\n"
             "value:    " (pr-str value)))))
  value)

(defn- project-set
  [template value]
  (assert-set! value template)
  (set (map #(project template %) value)))

;; ## Implementation

(extend-protocol Projection
  clojure.lang.IPersistentSet
  (project [sq value]
    {:pre [(= (count sq) 1)]}
    (let [template (first sq)]
      (then value #(project-set template %)))))
