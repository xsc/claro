(ns claro.data.tree.blocking-composition
  (:require [claro.data.protocols :as p])
  (:import [claro.data.protocols ResolvableTree ]))

(deftype BlockingComposition [tree f]
  ResolvableTree
  (wrapped? [this]
    true)
  (processable? [this]
    false)
  (unwrap-tree [this]
    this)
  (partial-value [_ no-partial]
    no-partial)
  (resolved? [_]
    false)
  (resolvables* [_]
    (p/resolvables* tree))
  (apply-resolved-values [this resolvable->value]
    (let [tree' (p/apply-resolved-values tree resolvable->value)]
      (cond (= tree tree') this
            (p/resolved? tree') (-> tree' f)
            :else (BlockingComposition. tree' f)))))
