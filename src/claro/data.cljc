(ns claro.data
  (:refer-clojure :exclude [update update-in select-keys])
  (:require [claro.data protocols chain ops projection tree]
            [potemkin :refer [import-vars]]))

(import-vars
  [claro.data.chain
   chain-when]

  [claro.data.ops
   update
   update-in
   select-keys
   update-keys
   then
   then!]

  [claro.data.projection
   project]

  [claro.data.protocols
   Resolvable
   BatchedResolvable
   resolvable?
   resolve!
   resolve-batch!])
