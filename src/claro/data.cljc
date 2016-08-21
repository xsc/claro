(ns claro.data
  (:require [claro.data protocols]
            [potemkin :refer [import-vars]]))

(import-vars
  [claro.data.protocols
   BatchedResolvable
   Mutation
   Resolvable
   resolvable?
   resolve!
   resolve-batch!])
