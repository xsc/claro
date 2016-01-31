(ns claro.data.tree.object
  (:require [claro.data.protocols :as p]))

(extend-protocol p/ResolvableTree
  Object
  (wrapped? [_]
    false)
  (unwrap-tree [this]
    this)
  (partial-value [tree _]
    tree)
  (resolved? [tree]
    true)
  (resolvables* [_]
    nil)
  (apply-resolved-values [tree _]
    tree)

  nil
  (wrapped? [_]
    false)
  (unwrap-tree [_]
    nil)
  (partial-value [_ _]
    nil)
  (resolved? [_]
    true)
  (resolvables* [_]
    nil)
  (apply-resolved-values [_ _]
    nil))
