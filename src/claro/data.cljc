(ns claro.data
  (:refer-clojure
    :exclude [assoc assoc-in drop first map nth
              select-keys take update update-in])
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
   drop
   first
   map
   nth
   take]

  [claro.data.ops.maps
   assoc
   assoc-in
   select-keys
   update
   update-in]

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
