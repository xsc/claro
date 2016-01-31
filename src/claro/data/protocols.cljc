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
  (wrapped? [tree]
    "Check whether the given tree is wrapped.")
  (unwrap-tree [tree]
    "Unwrap the given tree as far as possible.")
  (partial-value [tree no-partial]
    "Retrieve the potentially not fully-resolved value for this tree. Returns
     `no-partial` if this tree has not partial representation.")
  (resolved? [tree]
    "Is the tree completely resolved?")
  (resolvables* [tree]
    "Return a seq of all Resolvables within the given tree.")
  (apply-resolved-values [tree resolvable->resolved]
    "Replace the `Resolvables` with the given resolved values, returning a
     potentially fully resolved `ResolvableTree`."))

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
