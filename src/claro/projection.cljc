(ns claro.projection
  "Powerful tree projection functions.

   These will allow you to convert an infinite tree of `Resolvable` values to
   a finite form, performing transformations, injections and selection along the
   way.

   See [Projections][1] and [Advanced Projections][2] for a detailed discussion
   and examples.

   [1]: 01-projection.md
   [2]: 02-advanced-projection.md
   "
  (:refer-clojure :exclude [apply alias case let merge juxt])
  (:require [claro.projection
             [protocols :refer [project]]
             aux
             conditional
             case
             bind
             juxt
             level
             maps
             maybe
             objects
             parameters
             remove-nil
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
  [claro.projection.aux
   extract
   extract-in]
  [claro.projection.objects
   leaf]
  [claro.projection.conditional
   conditional
   conditional-union]
  [claro.projection.case
   case
   case-resolvable]
  [claro.projection.bind
   bind
   let]
  [claro.projection.juxt
   juxt*
   juxt]
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
  [claro.projection.remove-nil
   remove-nil-elements]
  [claro.projection.union
   merge*
   merge
   union*
   union]
  [claro.projection.value
   value
   finite-value])
