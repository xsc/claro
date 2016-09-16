(ns claro.projection
  (:refer-clojure :exclude [apply alias case let])
  (:require [claro.projection
             [protocols :refer [project]]
             conditional
             case
             bind
             level
             maps
             maybe
             objects
             parameters
             sequential
             sets
             transform
             union
             value]
            [potemkin :refer [import-vars]]))

;; ## Projection

(defn apply
  "Project the given value using the given template."
  [value template]
  (project template value))

;; ## API

(import-vars
  [claro.projection.objects
   leaf]
  [claro.projection.conditional
   conditional
   conditional-union]
  [claro.projection.case
   case]
  [claro.projection.bind
   bind
   let]
  [claro.projection.level
   levels]
  [claro.projection.maps
   alias]
  [claro.projection.maybe
   maybe
   default]
  [claro.projection.transform
   prepare
   transform]
  [claro.projection.parameters
   parameters]
  [claro.projection.union
   union*
   union]
  [claro.projection.value
   value])
