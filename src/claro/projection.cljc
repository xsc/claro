(ns claro.projection
  (:refer-clojure :exclude [apply])
  (:require [claro.projection
             [protocols :refer [project-template]]
              maps
              objects
              sequential]
            [potemkin :refer [import-vars]]))

;; ## Projection

(defn apply
  "Project the given value using the given template."
  [value template]
  (project-template template value))

;; ## API

(import-vars
  [claro.projection.objects leaf])
