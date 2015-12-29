(ns claro.data.tree
  (:require [claro.data.protocols :as p]
            [claro.data.tree
             [collection :refer [->ResolvableCollection]]
             [leaf :refer [->ResolvableLeaf]]
             [map-entry :refer [->ResolvableMapEntry]]
             [object]
             [utils :as u]]
            [potemkin :refer [defprotocol+]]))

(declare wrap-tree)

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
  (resolvable-collection (map ->map-entry) m))

;; ## Collections

(defn- list->tree
  [l]
  (resolvable-collection (map wrap-tree) (reverse l)))

(defn- collection->tree
  [coll]
  (resolvable-collection (map wrap-tree) coll))

;; ## Records

(defn- record->tree
  [record]
  (let [elements (into [] (map ->map-entry) record)
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
