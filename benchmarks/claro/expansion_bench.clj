(ns claro.expansion-bench
  (:require [perforate.core :refer [defgoal defcase]]
            [manifold.deferred :as d]
            [claro.data :as data]
            [claro.engine :as engine]))

(def run!! engine/run!!)

(defgoal expansion
  "Resolution of a Resolvable that expands up to a certain point.")

(defrecord ExpandedSeq [n max-n]
  data/Resolvable
  (resolve! [_ _]
    (d/future
      (if (< n max-n)
        {:tail (ExpandedSeq. (inc n) max-n)}
        {:head n}))))


(let [resolvable (ExpandedSeq. 0 16)]
  (defcase expansion :shallow-expansion
    []
    (run!! resolvable)))

(let [resolvable (ExpandedSeq. 0 255)]
  (defcase expansion :deep-expansion
    []
    (run!! resolvable)))
