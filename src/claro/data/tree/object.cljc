(ns claro.data.tree.object
  (:require [claro.data.protocols :as p]))

(extend-protocol p/ResolvableTree
  Object
  (unwrap-tree1 [tree]
    tree)
  (resolved? [tree]
    (not (p/resolvable? tree)))
  (resolvables* [_]
    nil)
  (apply-resolved-values [tree _]
    tree)

  nil
  (unwrap-tree1 [_]
    nil)
  (resolved? [_]
    true)
  (resolvables* [_]
    nil)
  (apply-resolved-values [_ _]
    nil))
