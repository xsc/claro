(ns claro.data
  (:refer-clojure
    :exclude [assoc assoc-in drop first get get-in map nth
              select-keys take update update-in])
  (:require [claro.data
             protocols]
            [claro.data.ops
             collections
             fmap
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

  [claro.data.ops.fmap
   fmap
   fmap-on
   fmap!]

  [claro.data.ops.maps
   assoc
   assoc-in
   get
   get-in
   select-keys
   update
   update-in]

  [claro.data.ops.then
   on
   then
   then!]

  [claro.data.protocols
   BatchedResolvable
   Mutation
   project
   Resolvable
   resolvable?
   resolve!
   resolve-batch!])
