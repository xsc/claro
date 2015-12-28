(ns claro.data.tree
  (:require [claro.data.resolvable :as r]
            [potemkin :refer [defprotocol+]]))

;; ## Protocols

(defprotocol+ Tree
  (wrap-tree [tree]
    "Wrap the given tree for `Resolvable` processing, returning a
     `ResolvableTree`."))

(defprotocol+ ResolvableTree
  (unwrap-tree1 [tree]
    "Unwrap one level of the (potentially not fully-resolved) tree value.")
  (resolved? [tree]
    "Is the tree completely resolved?")
  (resolvables* [tree]
    "Return a seq of all Resolvables within the given tree.")
  (apply-resolved-values [tree resolvable->resolved]
    "Replace the `Resolvables` with the given resolved values, returning a
     potentially fully resolved `ResolvableTree`."))

(defn resolvables
  [tree]
  (into #{} (resolvables* tree)))

;; ## Wrapped tree

(defprotocol+ WrappedTree
  "Maker for wrapped trees."
  (wrapped? [tree]))

(defn wrapped?
  [tree]
  (satisfies? WrappedTree tree))

;; ## Resolvable Tree

;; ### Helper

(defn- can-resolve?
  [tree resolvable->resolved]
  (and (not (resolved? tree))
       ;; it seems the following check is slower than just trying to apply the
       ;; resolution ...
       #_(some resolvable->resolved (resolvables tree))))

(defn- apply-resolution
  [tree resolvable->resolved]
  (if (can-resolve? tree resolvable->resolved)
    (apply-resolved-values tree resolvable->resolved)
    tree))

(def ^:private all-resolvables-xf
  "Transducer to collect all resolvables in a seq of `ResolvableTree`values."
  (mapcat #(resolvables* %)))

(defn- merge-resolvables
  ([trees]
   (into [] all-resolvables-xf trees))
  ([tree0 tree1]
   (into (resolvables* tree0) (resolvables* tree1))))

;; ### Leaves

(deftype ResolvableLeaf [resolvable]
  ResolvableTree
  (unwrap-tree1 [this]
    (.-resolvable this))
  (resolved? [_]
    false)
  (resolvables* [this]
    [(.-resolvable this)])
  (apply-resolved-values [tree resolvable->resolved]
    (get resolvable->resolved (.-resolvable tree) tree)))

;; ### Map Entries

(deftype ResolvableMapEntry [resolvables k v]
  ResolvableTree
  (unwrap-tree1 [tree]
    [(.-k tree) (.-v tree)])
  (resolved? [_]
    false)
  (resolvables* [tree]
    (.-resolvables tree))
  (apply-resolved-values [tree resolvable->value]
    (let [k' (apply-resolution (.-k tree) resolvable->value)
          v' (apply-resolution (.-v tree) resolvable->value)
          remaining (merge-resolvables k' v')]
      (if (empty? remaining)
        [k' v']
        (ResolvableMapEntry. remaining k' v')))))

;; ### Inner Node

(deftype ResolvableNode [resolvables prototype elements]
  ResolvableTree
  (unwrap-tree1 [_]
    (prototype (map unwrap-tree1 elements)))
  (resolved? [_]
    false)
  (resolvables* [tree]
    (.-resolvables tree))
  (apply-resolved-values [tree resolvable->value]
    (loop [elements     (seq elements)
           elements'    (transient [])
           resolvables' nil]
      (if elements
        (let [e (first elements)
              v (apply-resolution e resolvable->value)
              rs (claro.data.tree/resolvables v)]
          (recur
            (next elements)
            (conj! elements' v)
            (into resolvables' rs)))
        (if (empty? resolvables')
          (prototype (persistent! elements'))
          (ResolvableNode. resolvables' prototype (persistent! elements')))))))

;; ### Object + Nil

(extend-protocol ResolvableTree
  Object
  (unwrap-tree1 [tree]
    tree)
  (resolved? [_]
    true)
  (resolvables* [_]
    nil)
  (apply-resolved-values [tree _]
    tree)

  nil
  (unwrap-tree1 [_]
    nil)
  (resolved? [_]
    true)
  (resolvables* [_]
    nil)
  (apply-resolved-values [_ _]
    nil))

;; ## Wrappers

(defn- map-entry?
  [e]
  (instance? java.util.Map$Entry e))

(defn- ->leaf
  [tree]
  (when (r/resolvable? tree)
    (ResolvableLeaf. tree)))

(defn- ->map-entry
  [e]
  (when (instance? java.util.Map$Entry e)
    (let [k (wrap-tree (key e))
          v (wrap-tree (val e))
          resolvables (merge-resolvables k v)]
      (if (empty? resolvables)
        e
        (ResolvableMapEntry. resolvables k v)))))

(defn- ->node-prototype
  [coll]
  (if (record? coll)
    #(into coll %)
    (let [empty-coll (empty coll)]
      (if (list? coll)
        #(into empty-coll (reverse %))
        #(into empty-coll %)))))

(defn- ->node
  [coll]
  (when (coll? coll)
    (let [elements (map wrap-tree coll)
          resolvables (into [] all-resolvables-xf elements)]
      (if (empty? resolvables)
        coll
        (ResolvableNode.
          resolvables
          (->node-prototype coll)
          elements)))))

(extend-protocol Tree
  Object
  (wrap-tree [tree]
    (or (->leaf tree)
        (->map-entry tree)
        (->node tree)
        tree))

  nil
  (wrap-tree [_]
    nil))
