(ns claro.tree.combinator
  (:require [claro.tree
             [inner :refer [->CollectionNode]]
             [protocols :as tree]])
  (:import [claro.tree.inner.node InnerNode]))

;; ## Helper

(defn- partial-values
  [values-node not-found]
  (if (tree/unresolved? values-node)
    (let [values (.-value ^InnerNode values-node)
          it     (clojure.lang.RT/iter values)]
      (loop [result (transient [])]
        (if (.hasNext it)
          (let [value (tree/unwrap-partial (.next it) ::none)]
            (if (= value ::none)
              not-found
              (recur (conj! result value))))
          (persistent! result))))
    values-node))

(defn- partial-values-with-fold
  [values resolvable->result]
  (let [partials (partial-values values ::none)]
    (if (= partials ::none)
      (let [values'   (tree/fold values resolvable->result)
            partials' (partial-values values' ::none)]
        (if (= partials' ::none)
          values'
          partials'))
      partials)))

;; ## Node

(deftype CombinatorNode* [values fs]
  tree/Node
  (resolvables-into! [this sq]
    (tree/resolvables-into! values sq))
  (unresolved? [this]
    true)
  (fpartial* [this fs']
    (CombinatorNode*. values (into fs fs')))
  (fold [this resolvable->result]
    (let [partials (partial-values-with-fold values resolvable->result)]
      (if (tree/unresolved? partials)
        (CombinatorNode*. partials fs)
        (tree/reduce-fns-and-fold fs partials resolvable->result))))
  (unwrap [this not-possible]
    not-possible))

;; ## Constructor

(defn ->CombinatorNode
  [f values]
  (->CombinatorNode*
    (->CollectionNode (vec values))
    [(partial apply f)]))
