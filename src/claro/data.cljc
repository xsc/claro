(ns claro.data
  (:refer-clojure :exclude [update update-in select-keys map])
  (:require [claro.data
             protocols]
            [claro.data.ops
             collections
             maps
             then]
            [claro.data.projection
             maps
             objects
             sequential]
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

  [claro.data.protocols
   BatchedResolvable
   project
   Resolvable
   resolvable?
   resolve!
   resolve-batch!])
