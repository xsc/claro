(ns claro.data
  "Main protocols and functions for I/O abstraction.

   See [Basic Resolution][1] for details and examples.

   [1]: 00-basics.md
   "
  (:require [claro.data protocols error]
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
   cost
   mutation?
   resolvable?
   batched-resolvable?
   pure-resolvable?
   resolve!
   resolve-batch!
   set-parameters
   transform]
  [claro.data.error
   collect-errors
   error
   error?
   error-message
   error-data])
