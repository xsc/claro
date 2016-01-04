(ns claro.projection-bench
  (:require [perforate.core :refer [defgoal defcase]]
            [manifold.deferred :as d]
            [claro.data :as data]
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
  (data/project
    (InfiniteSeq. 0)
    (assoc-in {} (repeat max-n :tail) {:head nil})))

(defcase projection :shallow-projection
  []
  (run!! (infinite-seq 16)))

(defcase projection :deep-projection
  []
  (run!! (infinite-seq 255)))
