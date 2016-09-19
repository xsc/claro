(ns claro.data
  "Main protocols and functions for I/O abstraction.

   See [Basic Resolution][1] for details and examples.

   [1]: 00-basics.md
   "
  (:require [claro.data protocols]
            [potemkin :refer [import-vars]]))

(import-vars
  [claro.data.protocols
   BatchedResolvable
   Mutation
   Resolvable
   Transform
   resolvable?
   resolve!
   resolve-batch!
   transform])
