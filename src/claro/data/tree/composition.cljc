(ns claro.data.tree.composition
  (:require [claro.data.protocols :as p])
  (:import [claro.data.protocols ResolvableTree WrappedTree]))

(deftype ResolvableComposition [tree predicate f]
  WrappedTree
  (unwrap [this]
    this)

  ResolvableTree
  (unwrap-tree1 [this]
    this)
  (resolved? [_]
    false)
  (resolvables* [_]
    (p/resolvables* tree))
  (apply-resolved-values [this resolvable->value]
    (let [tree' (p/apply-resolved-values tree resolvable->value)]
      (cond (= tree tree') this
            (p/resolved? tree') (f tree')
            (p/wrapped? tree') (ResolvableComposition. tree' predicate f)
            :else (let [value (p/unwrap-tree1 tree')]
                    (if (and predicate (predicate value))
                      (f value)
                      (ResolvableComposition. tree' predicate f)))))))
