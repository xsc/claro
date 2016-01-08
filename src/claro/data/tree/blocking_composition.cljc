(ns claro.data.tree.blocking-composition
  (:require [claro.data.protocols :as p])
  (:import [claro.data.protocols ResolvableTree WrappedTree]))

(deftype BlockingComposition [tree f]
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
            (p/resolved? tree') (-> tree' f)
            :else (BlockingComposition. tree' f)))))
