(ns claro.projection
  (:refer-clojure :exclude [apply alias])
  (:require [claro.projection
             [protocols :refer [project-template]]
             alias
             conditional
             level
             maps
             objects
             sequential
             sets
             transform
             union]
            [potemkin :refer [import-vars]]))

;; ## Projection

(defn apply
  "Project the given value using the given template."
  [value template]
  (project-template template value))

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
  [claro.projection.transform
   transform
   transform-at]
  [claro.projection.union
   conditional-union
   union])
