(ns claro.projection-bench
  (:require [perforate.core :refer [defgoal defcase]]
            [manifold.deferred :as d]
            [claro.data :as data]
            [claro.projection :as projection]
            [claro.engine :as engine]))

(def run!! engine/run!!)

(defgoal projection
  "Resolution of a projected Resolvable.")

(defrecord InfiniteSeq [n]
  data/Resolvable
  (resolve! [_ _]
    (d/future
      {:head n, :tail (InfiniteSeq. (inc n))})))

(defn infinite-seq
  [max-n]
  (projection/apply
    (InfiniteSeq. 0)
    (assoc-in {} (repeat max-n :tail) {:head nil})))

(let [resolvable (infinite-seq 16)]
  (defcase projection :shallow-projection
    []
    (run!! resolvable)))

(let [resolvable (infinite-seq 255)]
  (defcase projection :deep-projection
    []
    (run!! resolvable)))
