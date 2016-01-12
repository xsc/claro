(ns claro.data.projection.objects
  (:require [claro.data.protocols :refer [Projection]]))

(extend-protocol Projection
  nil
  (project-template [_ value]
    value))
