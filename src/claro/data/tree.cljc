(ns claro.data.tree
  (:require [claro.data.protocols :as p]
            [claro.data.tree
             [collection :refer [->ResolvableCollection]]
             [leaf :refer [->ResolvableLeaf]]
             [map-entry :refer [->ResolvableMapEntry]]
             [object]
             [utils :as u]]))

(declare wrap-tree ->map-entry)

;; ## Transducers

(def ^:private map-entry-xf
  (map #(->map-entry %)))

(def ^:private tree-xf
  (map #(wrap-tree %)))

;; ## Helper

(defn- resolvable-collection
  ([xf original]
   (resolvable-collection xf original original))
  ([xf original elements]
   (let [elements (into [] xf elements)
         resolvables (into [] u/all-resolvables-xf elements)]
     (if (empty? resolvables)
       original
       (->ResolvableCollection resolvables (empty original) elements)))))

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
  (resolvable-collection tree-xf l (reverse l)))

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
  (let [value (cond-> value (p/wrapped? value) p/unwrap)]
    (cond (p/resolvable? value) (->ResolvableLeaf value)
          (record? value) (record->tree value)
          (map? value) (map->tree value)
          (list? value) (list->tree value)
          (seq? value) (list->tree value)
          (coll? value) (collection->tree value)
          :else value)))
