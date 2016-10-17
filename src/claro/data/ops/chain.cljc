(ns claro.data.ops.chain
  (:require [claro.data.protocols :as p]
            [claro.data.tree
             [blocking-composition :refer [->BlockingComposition]]
             [composition :as composition :refer [->ResolvableComposition]]
             [leaf :refer [->ResolvableLeaf]]]
            [claro.data.tree :refer [wrap-tree]]))

;; ## Resolved Node

(deftype ResolvedComposition [value f]
  p/ResolvableTree
  (wrapped? [_]
    true)
  (unwrap-tree [_]
    (f (p/unwrap-tree value))))

;; ## Chains

(defn- chain-resolvable-when
  [value predicate f]
  (when (p/resolvable? value)
    (->ResolvableComposition (->ResolvableLeaf value) predicate f)))

(defn- chain-tree-when
  [value predicate f]
  (let [tree (wrap-tree value)
        value' (composition/match-value tree predicate ::none)]
    (if (= value' ::none)
      (->ResolvableComposition tree predicate f)
      (->ResolvedComposition value' f))))

(defn chain-when
  "Apply the given function to the (potentially not fully-resolved) value
   once `predicate` is fulfilled."
  [value predicate f]
  (let [f' (comp wrap-tree f)]
    (or (chain-resolvable-when value predicate f')
        (chain-tree-when value predicate f'))))

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
