(ns claro.data.resolvable
  (:require [manifold.deferred :as d]
            [potemkin :refer [defprotocol+]]))

;; ## Protocols

(defprotocol+ Resolvable
  "Protocol for resolvable values."
  (resolve! [resolvable env]
    "Resolve the given value within the given environment, returning a manifold
     deferrable representing the resolution result. This might yield a structure
     containing more `Resolvable`s."))

(defprotocol+ BatchedResolvable
  "Protocol for resolution of multiple values at once."
  (resolve-batch! [resolvable env all-resolvables]
    "Resolve `all-resolvables` (which is a seq including `resolvable`), returning
     a manifold deferrable containing a seq with resolved values in-order."))

;; ## Derived Functions

(defn resolvable?
  "Check whether the given value implements the `Resolvable` protocol."
  [value]
  (satisfies? Resolvable value))

(defn resolve-if-possible!
  "Resolve the given value within the given environment iff it implements
   the `Resolvable` protocol, otherwise just return it."
  [value env]
  (if (resolvable? value)
    (resolve! value env)
    value))

;; ## Default Implementations

(extend-protocol Resolvable
  manifold.deferred.Deferred
  (resolve! [deferred _]
    deferred))

(extend-protocol BatchedResolvable
  Object
  (resolve-batch! [_ env all-resolvables]
    (apply d/zip (map #(resolve-if-possible! % env) all-resolvables))))
