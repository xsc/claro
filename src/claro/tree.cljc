(ns claro.tree
  (:require [claro.tree
             [blocking :refer [->BlockingNode]]
             [combinator :refer [->CombinatorNode]]
             [error :as error :refer [->ErrorNode]]
             [inner :refer [->CollectionNode ->MapNode ->RecordNode]]
             [leaf :refer [->LeafNode]]
             [resolvable :refer [->ResolvableNode]]
             [transformer :refer [->TransformerNode]]
             [protocols :as tree]]
            [claro.data.protocols :as p]
            [potemkin :refer [import-vars]]))

;; ## Derived Functionality

(defn- wrapping
  [f]
  (comp tree/wrap f))

(defn ^{:added "0.3.0"} fpartial-raw
  "Like [[fpartial]] but does not rewrap the resulting value, allowing
   for performance improvements if it is known that `f` does not produce
   any resolvables."
  [f value & more]
  (if (empty? more)
    (tree/fpartial* (tree/wrap value) [f])
    (->CombinatorNode
      (error/error-aware f)
      (cons value more))))

(defn ^{:added "0.3.0"} fpartial
  "Apply `f` to the given values once every value is partially resolved.

   ```clojure
   ;; TODO
   ```

   The resulting value will be rewrapped, i.e. it can contain new
   resolvables for further resolution. If you don't need this behaviour
   since you know that `f` does not return any resolvables, use
   [[fpartial-raw]."
  [f value & more]
  (apply fpartial-raw (wrapping f) value more))

(defn ^{:added "0.3.0"} fmap-raw
  "Like [[fmap]] but does not rewrap the resulting value, allowing for
   performance improvements if it is known that `f` does not produce
   any resolvables."
  [f value & more]
  (if (empty? more)
    (tree/fmap* (tree/wrap value) [f])
    (tree/fmap*
      (tree/wrap (vec (cons value more)))
      [(partial apply (error/error-aware f))])))

(defn ^{:added "0.3.0"} fmap
  "Apply `f` to the given values once every value is fully resolved.

   ```clojure
   ;; TODO
   ```

   The resulting value will be rewrapped, i.e. it can contain new
   resolvables for further resolution. If you don't need this behaviour
   since you know that `f` does not return any resolvables, use
   [[fmap-raw]."
  [f value & more]
  (apply fmap-raw (wrapping f) value more))

(defn ^{:added "0.3.0"} ftransform
  "Transform a tree node, if possible. As opposed to [[fpartial]] and [[fmap]],
   `f` will be called immediately on the value contained within the node.

   If there is no such thing as a \"current value\" (i.e. if there are already
   functions applied to the node), an exception will be thrown."
  [f value]
  (tree/ftransform* (tree/wrap value) (wrapping f)))

(defn ^{:added "0.3.0"} resolvables
  [value]
  (let [out (java.util.LinkedList.)]
    (tree/resolvables-into! (tree/wrap value) out)
    (into #{} out)))

(defn ^{:added "0.3.0"} resolved?
  [value]
  (not (tree/unresolved? value)))

;; ## Default Implementations

(tree/extend-defaults
  tree/Tree
  (wrap [this]
    this)

  tree/Node
  (resolvables-into! [this sq]
    sq)
  (unresolved? [this]
    false)
  (fpartial* [this fs]
    (->LeafNode this fs))
  (ftransform* [this f not-possible]
    (f this))
  (fold [this _]
    this)
  (unwrap [this _]
    this)

  tree/PartialNode
  (unwrap-partial [this not-possible]
    (tree/unwrap this not-possible))

  tree/ErrorNode
  (error? [this]
    false)
  (error-message [this]
    nil)
  (error-data [this]
    nil)

  tree/BlockingNode
  (fmap* [this fs]
    (->BlockingNode this fs))

  tree/TransformableNode
  (ftransform* [this f]
    (->TransformerNode f this)))

;; ## Wrapping

(extend-protocol tree/Tree
  ;; Node values are already wrapped
  claro.tree.protocols.Node
  (wrap [this]
    this)

  ;; Maps
  clojure.lang.IPersistentMap
  (wrap [this]
    (cond (not (record? this)) (->MapNode this)
          (p/resolvable? this) (->ResolvableNode this [])
          :else                (->RecordNode this)))

  ;; Collections
  clojure.lang.IPersistentCollection
  (wrap [this]
    (->CollectionNode this))

  ;; Resolvable values need to be wrapped in ResolvableNode
  claro.data.protocols.Resolvable
  (wrap [this]
    (->ResolvableNode this []))

  claro.data.protocols.BatchedResolvable
  (wrap [this]
    (->ResolvableNode this [])))

;; ## Imports

(import-vars
  [claro.tree.protocols
   wrap
   fold
   unresolved?
   error?
   error-message
   error-data]

  [claro.tree.error
   error
   collect-errors
   unless-error->
   unless-error->>])
