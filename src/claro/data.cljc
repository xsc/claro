(ns claro.data
  (:refer-clojure :exclude [update update-in select-keys map])
  (:require [claro.data
             protocols
             projection]
            [claro.data.ops
             collections
             maps
             then]
            [potemkin :refer [import-vars]]))

(import-vars
  [claro.data.ops.collections
   map]

  [claro.data.ops.maps
   update
   update-in
   select-keys
   update-keys]

  [claro.data.ops.then
   on
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
