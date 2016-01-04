(ns claro.projection-bench
  (:require [perforate.core :refer [defgoal defcase]]
            [manifold.deferred :as d]
            [claro.data :as data]
            [claro.engine :as engine]))

(def run!! engine/run!!)

(defgoal projection
  "Resolution of a projected Resolvable.")

;; ## Projection

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

;; ## Expansion

(defrecord ExpandedSeq [n max-n]
  data/Resolvable
  (resolve! [_ _]
    (d/future
      (if (< n max-n)
        {:tail (ExpandedSeq. (inc n) max-n)}
        {:head n}))))

(defn expanded-seq
  [max-n]
  (ExpandedSeq. 0 max-n))

(defcase projection :shallow-expansion
  []
  (run!! (expanded-seq 16)))

(defcase projection :deep-expansion
  []
  (run!! (expanded-seq 255)))
