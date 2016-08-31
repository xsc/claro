(ns claro.projection
  (:refer-clojure :exclude [apply alias])
  (:require [claro.projection
             [protocols :refer [project]]
             alias
             conditional
             level
             maps
             maybe
             objects
             parameters
             sequential
             sets
             transform
             union]
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
  [claro.projection.alias
   alias]
  [claro.projection.conditional
   conditional
   conditional-when]
  [claro.projection.level
   levels]
  [claro.projection.maybe
   maybe
   default]
  [claro.projection.transform
   prepare
   transform]
  [claro.projection.parameters
   parameters]
  [claro.projection.union
   conditional-union
   union])
