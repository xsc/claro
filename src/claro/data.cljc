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
   Mutation
   Resolvable
   Transform
   Parameters
   resolvable?
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
