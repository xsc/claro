(ns claro.data.tree.tuple
  (:require [claro.data.protocols :as p]
            [claro.data.tree.utils :as u])
  (:import [claro.data.protocols ResolvableTree]))

;; ## Tree Type

(deftype ResolvableTuple [resolvables elements]
  ResolvableTree
  (wrapped? [_]
    false)
  (processable? [_]
    false)
  (resolved? [_]
    false)
  (unwrap-tree [tree]
    tree)
  (partial-value [_ _]
    elements)
  (resolvables* [tree]
    (.-resolvables tree))
  (apply-resolved-values [tree resolvable->value]
    (let [element-count (int (count elements))]
      (loop [elements    (transient elements)
             resolvables (transient #{})
             index       (int 0)]
        (cond (< index element-count)
              (let [value        (.nth ^clojure.lang.ITransientVector elements index)
                    value'       (u/apply-resolution value resolvable->value)
                    resolvables' (p/resolvables* value')]
                (recur
                  (assoc! elements index value')
                  (reduce conj! resolvables resolvables')
                  (inc index)))

              (zero? (count resolvables))
              (persistent! elements)

              :else
              (ResolvableTuple.
                (vec (persistent! resolvables))
                (persistent! elements)))))))

;; ## Constructor

(defn make
  [wrap-fn elements]
  (if-not (empty? elements)
    (let [elements (into [] (map wrap-fn) elements)
          resolvables (into [] u/all-resolvables-xf elements)]
      (if-not (empty? resolvables)
        (ResolvableTuple. resolvables elements)
        elements))
    []))
