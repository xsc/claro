(ns claro.data.tree
  (:require [claro.data.protocols :as p]
            [claro.data.tree
             [blocking-composition :refer [->BlockingComposition]]
             [collection :refer [->ResolvableCollection]]
             [composition :refer [->ResolvableComposition]]
             [leaf :refer [->ResolvableLeaf]]
             [map-entry :refer [->ResolvableMapEntry]]
             [object]
             [utils :as u]]
            [potemkin :refer [defprotocol+]]))

(declare wrap-tree ->map-entry)

;; ## Transducers

(def ^:private map-entry-xf
  (map #(->map-entry %)))

(def ^:private tree-xf
  (map #(wrap-tree %)))

;; ## Helper

(defn- resolvable-collection
  [xf original]
  (let [elements (into [] xf original)
        resolvables (into [] u/all-resolvables-xf elements)]
    (if (empty? resolvables)
      original
      (->ResolvableCollection resolvables (empty original) elements))))

;; ## Maps

(defn- ->map-entry
  [e]
  (let [k (wrap-tree (key e))
        v (wrap-tree (val e))
        resolvables (u/merge-resolvables k v)]
    (if (empty? resolvables)
      e
      (->ResolvableMapEntry resolvables k v))))

(defn- map->tree
  [m]
  (resolvable-collection map-entry-xf m))

;; ## Collections

(defn- list->tree
  [l]
  (resolvable-collection tree-xf (reverse l)))

(defn- collection->tree
  [coll]
  (resolvable-collection tree-xf coll))

;; ## Records

(defn- record->tree
  [record]
  (let [elements (into [] map-entry-xf record)
        resolvables (into [] u/all-resolvables-xf elements)]
    (if (empty? resolvables)
      record
      (->ResolvableCollection resolvables record elements))))

;; ## Wrappers

(defn wrap-tree
  [value]
  (cond (p/resolvable? value) (->ResolvableLeaf value)
        (record? value) (record->tree value)
        (map? value) (map->tree value)
        (list? value) (list->tree value)
        (coll? value) (collection->tree value)
        :else value))

;; ## Base Compositions

(defn chain-when
  "Apply the given function to the (potentially not fully-resolved) value
   once `predicate` is fulfilled."
  [value predicate f]
  (let [f' (comp wrap-tree f)]
    (if (and (not (p/resolvable? value)) (p/resolved? value))
      (if (predicate value)
        (f' value)
        (throw
          (IllegalStateException.
            (format "'predicate' does not hold for fully resolved: %s"
                    (pr-str value)))))
      (->ResolvableComposition (wrap-tree value) predicate f'))))

(defn chain-blocking
  "Apply the given function once `value` is fully resolved."
  [value f]
  (let [f' (comp wrap-tree f)]
    (if (p/resolved? value)
      (f' value)
      (->BlockingComposition (wrap-tree value) f'))))
