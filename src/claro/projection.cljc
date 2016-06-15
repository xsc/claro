(ns claro.projection
  (:refer-clojure :exclude [apply])
  (:require [claro.projection
             [protocols :refer [project-template]]
             conditional
             maps
             objects
             sequential
             sets
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
  [claro.projection.conditional
   conditional]
  [claro.projection.union
   conditional-union
   union])
