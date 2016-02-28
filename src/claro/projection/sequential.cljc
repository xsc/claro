(ns claro.projection.sequential
  (:require [claro.projection.protocols :refer [Projection project-template]]
            [claro.data.ops
             [collections :as c]
             [then :refer [then]]
             [maps :as m]]))

;; ## Helpers

(defn- assert-sequential!
  [value]
  (when-not (sequential? value)
    (throw
      (IllegalArgumentException.
        (str "projection template is sequential but value is not: "
             (pr-str value)))))
  value)

(defn- project-elements
  [template sq]
  (c/map #(project-template template %) sq))

;; ## Implementation

(extend-protocol Projection
  clojure.lang.Sequential
  (project-template [[template :as sq] value]
    {:pre [(= (count sq) 1)]}
    (then
      value
      (comp #(project-elements template %)
            assert-sequential!))))
