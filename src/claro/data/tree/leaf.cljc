(ns claro.data.tree.leaf
  (:require [claro.data.protocols :as p])
  (:import [claro.data.protocols ResolvableTree]))

(deftype ResolvableLeaf [resolvable]
  ResolvableTree
  (unwrap-tree1 [this]
    (.-resolvable this))
  (resolved? [_]
    false)
  (resolvables* [this]
    [(.-resolvable this)])
  (apply-resolved-values [tree resolvable->resolved]
    (get resolvable->resolved (.-resolvable tree) tree)))
