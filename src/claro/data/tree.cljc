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

(defn- reassemble-collection
  [original elements]
  (into (empty original) elements))

(defn- resolvable-collection
  ([xf original]
   (resolvable-collection xf original original))
  ([xf original elements]
   (let [elements (into [] xf elements)
         resolvables (into [] u/all-resolvables-xf elements)]
     (if (empty? resolvables)
       (reassemble-collection original elements)
       (->ResolvableCollection resolvables (empty original) elements)))))

;; ## Maps

(defn- reassemble-map-entry
  [e old-k old-v new-k new-v]
  (if (or (not= old-k new-k) (not= old-v new-v))
    [new-k new-v]
    e))

(defn- ->map-entry
  [e]
  (let [k (key e)
        v (val e)
        k' (wrap-tree k)
        v' (wrap-tree v)
        resolvables (u/merge-resolvables k' v')]
    (if (empty? resolvables)
      (reassemble-map-entry e k v k' v')
      (->ResolvableMapEntry resolvables k' v'))))

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

(defn- reassemble-record
  [record elements]
  (into record elements))

(defn- record->tree
  [record]
  (let [elements (into [] map-entry-xf record)
        resolvables (into [] u/all-resolvables-xf elements)]
    (if (empty? resolvables)
      (reassemble-record record elements)
      (->ResolvableCollection resolvables record elements))))

;; ## Wrappers

(defn wrap-tree
  [value]
  (let [value (p/unwrap-all value)]
    (cond (p/resolvable? value) (->ResolvableLeaf value)
          (record? value) (record->tree value)
          (map? value) (map->tree value)
          (list? value) (list->tree value)
          (seq? value) (list->tree value)
          (coll? value) (collection->tree value)
          :else value)))
