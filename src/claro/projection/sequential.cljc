(ns claro.projection.sequential
  (:require [claro.projection.protocols :refer [Projection project]]
            [claro.data.ops
             [collections :as c]
             [then :refer [then]]]))

;; ## Helpers

(defn- assert-sequential!
  [value template]
  (when-not (sequential? value)
    (throw
      (IllegalArgumentException.
        (str "projection template is sequential but value is not.\n"
             "template: [" (pr-str template) "]\n"
             "value:    " (pr-str value)))))
  value)

(defn- project-elements
  [template value]
  (assert-sequential! value template)
  (let [vs (map #(project template %) value)]
    (if (or (list? value) (seq? value))
      (list* vs)
      (into (empty value) vs))))

;; ## Implementation

(extend-protocol Projection
  clojure.lang.Sequential
  (project [[template :as sq] value]
    {:pre [(= (count sq) 1)]}
    (then value #(project-elements template %))))
