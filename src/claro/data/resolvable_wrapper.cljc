(ns claro.data.resolvable-wrapper
  (:require [claro.data.resolvable :as r]
            [potemkin :refer [defprotocol+]]))

;; ## Protocol

(defprotocol+ ResolvableWrapper
  "Protocol for wrappers around resolvable values (post-processors, etc...)."
  (resolvables [resolvable-wrapper]
    "Get all (distinct) resolvables in the given wrapper.")
  (apply-resolved [resolvable-wrapper resolved-values]
    "Given a map of `Resolvable` -> resolved value, create a new instance
     of the wrapper with resolved values in place of the original resolvables."))

(defprotocol+ WrappedResolvable
  "Marker Protocol for wrapped Resolvables, representing
   additional logic to be run on unwrapping.")

;; ## Derived Functions

(defn resolved?
  [value]
  (empty? (resolvables value)))

(defn wrapped?
  [value]
  (satisfies? WrappedResolvable value))

;; ## Default Implementations

(extend-protocol ResolvableWrapper
  clojure.lang.IPersistentCollection
  (resolvables [coll]
    (cond (r/resolvable? coll) [coll]
          (wrapped? coll) (resolvables coll)
          :else (distinct (mapcat resolvables coll))))
  (apply-resolved [coll resolved-values]
    (cond (r/resolvable? coll) (get resolved-values coll coll)
          (wrapped? coll) (apply-resolved coll resolved-values)
          :else (into (if (record? coll) coll (empty coll))
                      (map #(apply-resolved % resolved-values) coll))))

  clojure.lang.MapEntry
  (resolvables [[k v]]
    (distinct
      (concat
        (resolvables k)
        (resolvables v))))
  (apply-resolved [[k v] resolved-values]
    (clojure.lang.MapEntry.
      (apply-resolved k resolved-values)
      (apply-resolved v resolved-values)))

  Object
  (resolvables [o]
    (when (r/resolvable? o)
      [o]))
  (apply-resolved [o resolved-values]
    (get resolved-values o o))

  nil
  (resolvables [_]
    nil)
  (apply-resolved [_ _]
    nil))
