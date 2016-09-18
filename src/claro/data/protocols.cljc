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

;; ## Postprocessing
;;
;; Splitting up data retrieval and processing, allows for code reuse and
;; increases testability, since we only have to mock a very limited part of the
;; I/O logic.

(defprotocol+ Transform
  "Protocol for post-processing the result of [[resolve!]] and the single
   results of [[resolve-batch!]]."
  (transform [resolvable resolve-result]
    "Transform the result of [[resolve!]] (or a single result of
     [[resolve-batch!]]) for the current [[Resolvable]] class.."))

(extend-protocol Transform
  Object
  (transform [_ resolve-result]
    resolve-result))

;; ## Mutations

(defprotocol+ Mutation
  "Marker interface for mutations, applying the following constraints on
   resolution:

   - only resolve on top-level (i.e. `resolve!` cannot return a mutation),
   - only resolve one mutation per run,
   - run mutation before any other resolvables.

   Do not use with `extend-type` or `extend-protocol`.")

(defn mutation?
  [value]
  (instance? claro.data.protocols.Mutation value))

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
  "Return a seq of resolvables from the given tree."
  [tree]
  (resolvables* tree))

(defn processable?
  "Check whether the given value is neither wrapped, nor resolvable."
  [value]
  (not (or (resolvable? value) (wrapped? value))))
