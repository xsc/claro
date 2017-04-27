(ns claro.data.tree.map
  (:require [claro.data.protocols :as p]
            [claro.data.tree
             [tuple :as tuple]
             [utils :as u]])
  (:import [claro.data.protocols ResolvableTree]))

;; ## Collector

(defn- collect-resolvables
  [keys-tuple vals-tuple]
  (into
    (vec (p/resolvables* keys-tuple))
    (p/resolvables* vals-tuple)))

;; ## Tree Type

(deftype ResolvableMap [constructor resolvables keys-tuple vals-tuple]
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
    (constructor
      (p/partial-value keys-tuple ::none)
      (p/partial-value vals-tuple ::none)))
  (resolvables* [tree]
    (.-resolvables tree))
  (apply-resolved-values [tree resolvable->values]
    (let [keys-tuple' (u/apply-resolution keys-tuple resolvable->values)
          vals-tuple' (u/apply-resolution vals-tuple resolvable->values)]
      (if (and (p/resolved? keys-tuple')
               (p/resolved? vals-tuple'))
        (constructor keys-tuple' vals-tuple')
        (ResolvableMap.
          constructor
          (collect-resolvables keys-tuple' vals-tuple')
          keys-tuple'
          vals-tuple')))))

(defmethod print-method ResolvableMap
  [^ResolvableMap value ^java.io.Writer writer]
  (print-method (p/partial-value value ::none) writer))

;; ## Constructor

(defn- make-sub-tuple
  [resolvables elements]
  (if (zero? (count resolvables))
    (persistent! elements)
    (tuple/->ResolvableTuple
      (persistent! resolvables)
      (persistent! elements))))

(defn make
  [wrap-fn constructor map-value]
  (if-not (empty? map-value)
    (loop [m               (seq map-value)
           keys            (transient [])
           vals            (transient [])
           key-resolvables (transient [])
           val-resolvables (transient [])
           resolvables     (transient [])]
      (cond m
            (let [e (first m)
                  k (wrap-fn (key e))
                  v (wrap-fn (val e))
                  kr (p/resolvables* k)
                  vr (p/resolvables* v)]
              (recur
                (next m)
                (conj! keys k)
                (conj! vals v)
                (reduce conj! key-resolvables kr)
                (reduce conj! val-resolvables vr)
                (as-> resolvables <>
                  (reduce conj! <> kr)
                  (reduce conj! <> vr))))

            (zero? (count resolvables))
            (constructor
              (persistent! keys)
              (persistent! vals))

            :else
            (ResolvableMap.
              constructor
              (persistent! resolvables)
              (make-sub-tuple key-resolvables keys)
              (make-sub-tuple val-resolvables vals))))
    {}))
