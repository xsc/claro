(ns claro.data.protocols
  (:refer-clojure :exclude [partition-by])
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
  "Interface for values that can be resolved in batches.

   Do not use with `extend-type` or `extend-protocol`!"
  (resolve-batch! [resolvable env all-resolvables]
    "Resolve `all-resolvables` (which is a seq including `resolvable`), returning
     a manifold deferrable containing a seq with resolved values in-order."))

(defprotocol+ PureResolvable
  "Interface for values whose resolution is done using a pure function, i.e.
   there is no I/O or statefulness involved.

   Do not use with `extend-type` or `extend-protocol`!")

(defn ^{:added "0.2.7"} batched-resolvable?
  "Check whether the given value implements the `Resolvable` protocol."
  [value]
  (instance? claro.data.protocols.BatchedResolvable value))

(defn ^{:added "0.2.7"} pure-resolvable?
  "Check whether the given value implements the `PureResolvable` protocol."
  [value]
  (instance? claro.data.protocols.PureResolvable value))

(defn resolvable?
  "Check whether the given value implements the `Resolvable` protocol.

   NOTE: Before version 0.2.7 this did not return true for `BatchedResolvable`
         records."
  [value]
  (or (instance? claro.data.protocols.Resolvable value)
      (batched-resolvable? value)))

;; ## Costs

(defprotocol+ ^{:added "0.2.6"} Cost
  "Protocol describing resolution cost for a batch of resolvables. The
   following default values are produced:

   - `0` for batches of `PureResolvable`,
   - `1` for batches of `BatchedResolvables`
   - `(count batch)` for other batches.

   The overall cost can be used when declaring engines by supplying the
   `:max-cost` option."
  (^{:added "0.2.6"} cost [resolvable batch]
    "Calculate the cost of resolution for the given batch."))

(extend-protocol Cost
  Object
  (cost [resolvable batch]
    (cond (pure-resolvable? resolvable)    0
          (batched-resolvable? resolvable) 1
          :else (count batch))))

;; ## Partitions

(defprotocol+ ^{:added "0.2.10"} Partition
  "Protocol enhancing batching capabilities by allowing batches to be
   partitioned using some per-resolvable-class criterion.

   This is only used for batched resolvables."
  (partition-by [resolvable]
    "Calculate a partitioning key for the given resolvable. Resolvables with
     the same partitioning key will be resolved within the same batch."))

(extend-protocol Partition
  Object
  (partition-by [_]
    nil))

(defn partition-batch
  "Partition a batch of resolvables using the [[Partition]] protocol"
  [batch]
  (->> batch
       (group-by partition-by)
       (vals)))

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

(defn ^{:added "0.2.7"} mutation?
  [value]
  (instance? claro.data.protocols.Mutation value))

;; ## Parameterization

(defprotocol+ Parameters
  "Protocol for custom parameter handling for Resolvables."
  (set-parameters [this parameters]
    "Set the current resolvable's parameters, returning an updated
     resolvable.

     This is used by the [[parameters]] projection for injection."))

;; ## Trees

(defprotocol+ ResolvableTree
  (wrapped? [tree]
    "Check whether the given tree is wrapped.")
  (processable? [tree]
    "Check whether the given tree already has the shape of its final value,
     albeit with potentially partially resolved children.")
  (unwrap-tree [tree]
    "Unwrap the given tree as far as possible.")
  (partial-value [tree no-partial]
    "Retrieve the potentially not fully-resolved value for this tree. Returns
     `no-partial` if this tree has no partial representation.")
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

(defn every-processable?
  "Check whether every value in the given collection is processable."
  [sq]
  (every? processable? sq))
