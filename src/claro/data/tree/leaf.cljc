(ns claro.data.tree.leaf
  (:require [claro.data.protocols :as p])
  (:import [claro.data.protocols ResolvableTree]))

(deftype ResolvableLeaf [resolvable]
  ResolvableTree
  (wrapped? [_]
    false)
  (processable? [_]
    false)
  (unwrap-tree [this]
    this)
  (partial-value [this _]
    (.-resolvable this))
  (resolved? [_]
    false)
  (resolvables* [this]
    [(.-resolvable this)])
  (apply-resolved-values [tree resolvable->resolved]
    (get resolvable->resolved (.-resolvable tree) tree)))

(defmethod print-method ResolvableLeaf
  [^ResolvableLeaf value ^java.io.Writer writer]
  (print-method (.-resolvable value) writer))
