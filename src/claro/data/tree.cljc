(ns claro.data.tree
  (:require [claro.data.protocols :as p]
            [claro.data.tree
             [collection :as collection]
             [map :as map]
             [leaf :as leaf]
             [object]]
            [potemkin :refer [defprotocol+]]))

;; ## Logic

(defprotocol+ ^:private TreeWrapper
  (wrap-tree [value]))

(defn transform-partial
  [value f]
  (-> value
      (p/partial-value nil)
      (f)
      (wrap-tree)))

;; ## Collections

(defn- collection->tree
  [c]
  (if-not (empty? c)
    (let [prototype (empty c)]
      (collection/make wrap-tree #(into prototype %) c))
    c))

(defn- vector->tree
  [v]
  (if-not (empty? v)
    (collection/make wrap-tree identity v)
    v))

(defn- set->tree
  [s]
  (if-not (empty? s)
    (collection/make wrap-tree set s)
    s))

(defn- list->tree
  [l]
  (if-not (empty? l)
    (collection/make wrap-tree list* l)
    l))

;; ## Maps

(defn- reassemble-record
  [record keys vals]
  (into record (map vector keys vals)))

(defn- record->tree
  [record]
  (map/make
    wrap-tree
    #(reassemble-record record %1 %2)
    record))

(defn- map->tree
  [m]
  (map/make wrap-tree zipmap m))

;; ## Wrappers

(extend-protocol TreeWrapper
  clojure.lang.IPersistentMap
  (wrap-tree [value]
    (cond (p/resolvable? value) (leaf/make value)
          (record? value) (record->tree value)
          :else (map->tree value)))

  clojure.lang.IPersistentVector
  (wrap-tree [value]
    (vector->tree value))

  clojure.lang.IPersistentSet
  (wrap-tree [value]
    (set->tree value))

  clojure.lang.IPersistentList
  (wrap-tree [value]
    (list->tree value))

  clojure.lang.ISeq
  (wrap-tree [value]
    (list->tree value))

  clojure.lang.IPersistentCollection
  (wrap-tree [value]
    (collection->tree value))

  Object
  (wrap-tree [value]
    (let [value' (p/unwrap-tree value)]
      (cond (p/resolvable? value')
            (leaf/make value')

            (identical? value value')
            value

            :else
            (wrap-tree value'))))

  nil
  (wrap-tree [_]
    nil))
