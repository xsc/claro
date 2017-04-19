(ns claro.data.tree.composition
  (:require [claro.data.protocols :as p])
  (:import [claro.data.protocols ResolvableTree]))

;; ## Helper

(defn- throw-resolved-without-predicate!
  [value predicate]
  (throw
    (IllegalStateException.
      (format "predicate %s does not hold for fully resolved: %s"
              (pr-str predicate)
              (pr-str value)))))

(defn- match-simple-value
  [value predicate no-match]
  (if (or (not predicate)
          (predicate value))
    value
    no-match))

(defn- match-resolved-value
  [value predicate no-match]
  (let [result (match-simple-value value predicate no-match)]
    (if (not= result no-match)
      result
      (throw-resolved-without-predicate! value predicate))))

(defn- match-partial-value
  [value predicate no-match]
  (let [value' (p/partial-value value ::none)]
    (if (and (not= value' ::none)
             (not (p/resolvable? value')))
      (match-simple-value value' predicate no-match)
      no-match)))

(defn match-value
  [value predicate no-match]
  (cond (or (p/resolvable? value)
            (p/wrapped? value))
        no-match

        (p/resolved? value)
        (match-resolved-value value predicate no-match)

        :else
        (let [value (match-partial-value value predicate ::none)]
          (if (not= value ::none)
            value
            no-match))))

;; ## Resolvable Node

(deftype ResolvableComposition [tree predicate f]
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
      (if-not (identical? tree tree')
        (let [value (match-value tree' predicate ::none)]
          (if (not= value ::none)
            (f value)
            (ResolvableComposition. tree' predicate f)))
        this))))

(defmethod print-method ResolvableComposition
  [^ResolvableComposition value ^java.io.Writer writer]
  (.write writer "<< ")
  (print-method (.-tree value) writer)
  (.write writer " => ... >>"))
