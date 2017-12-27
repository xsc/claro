(ns claro.tree.inner
  (:require [claro.tree.inner
             [map-walker :refer [map-walker]]
             [navigators :as nav]
             [tree-walker :refer [tree-walker]]]
            [claro.tree.protocols :as tree]
            [com.rpl.specter :as specter]))

;; ## Walkers

(def ^:private collection-walker
  (tree-walker
    (specter/comp-paths specter/ALL)))

(def ^:private vector-walker
  (tree-walker
    (specter/comp-paths nav/VECTOR-ALL)))

(def ^:private list-walker
  (tree-walker
    (specter/comp-paths nav/LIST-ALL)))

(def ^:private record-walker
  (tree-walker
    (specter/comp-paths nav/RECORD-ALL)))

;; ## Constructors

(defn ->MapNode
  [value]
  (map-walker value tree/wrap))

(defn ->RecordNode
  [value]
  (record-walker value tree/wrap))

(defn ->ListNode
  [value]
  (list-walker value tree/wrap))

(defn ->VectorNode
  [value]
  (vector-walker value tree/wrap))

(defn ->CollectionNode
  [value]
  (cond (vector? value) (->VectorNode value)
        (list? value)   (->ListNode value)
        :else           (collection-walker value tree/wrap)))
