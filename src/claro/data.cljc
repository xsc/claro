(ns claro.data
  (:refer-clojure :exclude [update update-in select-keys])
  (:require [claro.data
             protocols
             projection]
            [claro.data.ops
             chain
             maps
             then]
            [potemkin :refer [import-vars]]))

(import-vars
  [claro.data.ops.chain
   chain-when]

  [claro.data.ops.then
   then
   then!]

  [claro.data.ops.maps
   update
   update-in
   select-keys
   update-keys]

  [claro.data.projection
   project]

  [claro.data.protocols
   Resolvable
   BatchedResolvable
   resolvable?
   resolve!
   resolve-batch!])
