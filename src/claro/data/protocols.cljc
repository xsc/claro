(ns claro.data.protocols
  (:require [potemkin :refer [defprotocol+ definterface+]]))

;; ## Resolvables

(defprotocol+ Resolvable
  "Interface for resolvable values.

   Do not use with `extend-type` or `extend-protocol`!"
  (resolve! [resolvable env]
    "Resolve the given value within the given environment, returning a manifold
     deferrable representing the resolution result. This might yield a structure
     containing more `Resolvable`s."))

(defprotocol+ BatchedResolvable
  "Interface for values that can be resolved in batches. These must also implement
   the `Resolvable` interface.

   Do not use with `extend-type` or `extend-protocol`!"
  (resolve-batch! [resolvable env all-resolvables]
    "Resolve `all-resolvables` (which is a seq including `resolvable`), returning
     a manifold deferrable containing a seq with resolved values in-order."))

(defn resolvable?
  "Check whether the given value implements the `Resolvable` protocol."
  [value]
  (instance? claro.data.protocols.Resolvable value))

(defn batched-resolvable?
  "Check whether the given value implements the `Resolvable` protocol."
  [value]
  (instance? claro.data.protocols.BatchedResolvable value))

;; ## Trees

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

(definterface+ WrappedTree
  (unwrap [this]
    "Unwrap the given value to produce the actual tree element."))

(defn wrapped?
  "Check whether the given value is wrapped."
  [tree]
  (instance? WrappedTree tree))

(defn unwrap-all
  [value]
  (if (wrapped? value)
    (recur (unwrap value))
    value))

(defn resolvables
  "Return a set of resolvables from the given tree."
  [tree]
  (into #{} (resolvables* tree)))

(defn processable?
  "Check whether the given value is neither wrapped, nor resolvable."
  [value]
  (not (or (resolvable? value) (wrapped? value))))

;; ## Projection

(defprotocol+ Projection
  "Protocol for projection templates."
  (project-template [template value]
    "Use the given template to ensure the shape of the given value."))

(defn project
  "Project the given value using the given template."
  [value template]
  (project-template template value))
