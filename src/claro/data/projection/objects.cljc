(ns claro.data.projection.objects
  (:require [claro.data.protocols :refer [Projection]]))

;; ## Templates

(def leaf
  "Projection template for leaf values (equivalent to `nil` but preferable
   since more explicit)."
  (reify Projection
    (project-template [_ value]
      value)))

(extend-protocol Projection
  nil
  (project-template [_ value]
    value))
