(ns claro.data.ops.chain
  (:require [claro.data.protocols :as p]
            [claro.data.tree
             [blocking-composition :refer [->BlockingComposition]]
             [composition
              :refer [->ResolvableComposition matches? tree-matches?]]
             [leaf :refer [->ResolvableLeaf]]]
            [claro.data.tree :refer [wrap-tree]]))

;; ## Resolved Node

(deftype ResolvedComposition [value f]
  claro.data.protocols.WrappedTree
  (unwrap [_]
    (f (p/unwrap-all value))))

;; ## Helpers

(defn every-processable?
  "Check whether every value in the given collection is processable."
  [sq]
  (every? p/processable? sq))

;; ## Chains

(defn chain-when
  "Apply the given function to the (potentially not fully-resolved) value
   once `predicate` is fulfilled."
  [value predicate f]
  (let [f' (comp wrap-tree f)]
    (cond (p/resolvable? value)
          (->ResolvableComposition (->ResolvableLeaf value) predicate f')

          (matches? value predicate)
          (->ResolvedComposition value f')

          :else
          (let [tree (wrap-tree value)]
            (if (tree-matches? tree predicate)
              (->ResolvedComposition value f')
              (->ResolvableComposition tree predicate f'))))))

(defn chain-blocking
  "Apply the given function once `value` is fully resolved."
  [value f]
  (let [f' (comp wrap-tree f)]
    (if (p/resolvable? value)
      (->BlockingComposition (->ResolvableLeaf value) f')
      (let [tree (wrap-tree value)]
        (if (p/resolved? tree)
          (->ResolvedComposition tree f')
          (->BlockingComposition tree f'))))))

(defn chain-eager
  "Apply the given function once the value is no longer a `Resolvable` or
   wrapped."
  [value f]
  (chain-when value nil f))
