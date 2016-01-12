(ns claro.data.projection.sequential
  (:require [claro.data.protocols :refer [Projection project-template]]
            [claro.data.ops
             [collections :as c]
             [maps :as m]]))

(extend-protocol Projection
  clojure.lang.Sequential
  (project-template [[template :as sq] value]
    {:pre [(= (count sq) 1)]}
    (if (and (= (count sq) 1) (satisfies? Projection template))
      (c/map #(project-template template %) value)
      (m/select-keys value sq))))
