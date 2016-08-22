(ns claro.projection.objects
  (:require [claro.projection.protocols :refer [Projection]]
            [claro.data.ops.chain :refer [chain-eager]]))

;; ## Helpers

(defn- leaf?
  [value]
  (not (coll? value)))

(defn- assert-leaf
  [value]
  (when-not (leaf? value)
    (throw
      (IllegalStateException.
        (format
          (str "leaf projection template can only be used for non-collection "
               "values, given %s:%n%s ")
          (.getName (class value))
          (pr-str value)))))
  value)

;; ## Templates

(def leaf
  "Projection template for leaf values (equivalent to `nil` but preferable
   since more explicit)."
  (reify Projection
    (project [_ value]
      (chain-eager value assert-leaf))))

(extend-protocol Projection
  nil
  (project [_ value]
    (chain-eager value assert-leaf)))
