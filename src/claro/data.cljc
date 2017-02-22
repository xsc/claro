(ns claro.data
  "Main protocols and functions for I/O abstraction.

   See [Basic Resolution][1] for details and examples.

   [1]: 00-basics.md
   "
  (:refer-clojure :exclude [partition-by])
  (:require [claro.data protocols error transform]
            [potemkin :refer [import-vars]]))

(import-vars
  [claro.data.protocols
   BatchedResolvable
   Cost
   Mutation
   PureResolvable
   Resolvable
   Transform
   Parameters
   Partition
   cost
   mutation?
   resolvable?
   batched-resolvable?
   pure-resolvable?
   resolve!
   resolve-batch!
   set-parameters
   partition-by
   transform]
  [claro.data.error
   collect-errors
   error
   error?
   error-message
   error-data
   unless-error->
   unless-error->>]
  [claro.data.transform
   extend-list-transform
   extend-transform])
