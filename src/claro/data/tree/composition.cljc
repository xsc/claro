(ns claro.data.tree.composition
  (:require [claro.data.protocols :as p])
  (:import [claro.data.protocols ResolvableTree WrappedTree]))

;; ## Helper

(defn matches?
  [value predicate]
  (and (p/resolved? value)
       (or (not predicate)
           (predicate value))))

(defn tree-matches?
  [value predicate]
  (and (p/resolved? value)
       (or (not predicate)
           (predicate value)
           (throw
             (IllegalStateException.
               (format "predicate %s does not hold for fully resolved: %s"
                       (pr-str predicate)
                       (pr-str value)))))))

;; ## Resolvable Node

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
      (cond (identical? tree tree') this
            (tree-matches? tree' predicate) (f tree')
            (p/wrapped? tree') (ResolvableComposition. tree' predicate f)
            :else (let [value (p/unwrap-tree1 tree')]
                    (if (or (not predicate) (predicate value))
                      (f value)
                      (ResolvableComposition. tree' predicate f)))))))
