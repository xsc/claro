(ns claro.data
  (:refer-clojure :exclude [update update-in select-keys])
  (:require [claro.data protocols ops projection tree]
            [potemkin :refer [import-vars]]))

(import-vars
  [claro.data.tree
   chain-when]

  [claro.data.ops
   update
   update-in
   select-keys
   update-keys
   chain
   wait]

  [claro.data.projection
   project]

  [claro.data.protocols
   Resolvable
   BatchedResolvable
   resolvable?
   resolve!
   resolve-batch!])
