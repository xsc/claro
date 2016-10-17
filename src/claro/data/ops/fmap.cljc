(ns claro.data.ops.fmap
  (:require [claro.data.protocols :as p]
            [claro.data.ops.chain :as chain]))

(defn fmap*
  "Apply the given function to the given, potentially partially resolved
   seq of values."
  [f values]
  (chain/chain-when
    (vec values)
    p/every-processable?
    #(apply f %)))

(defn fmap
  "Apply the given function to the given, potentially partially resolved
   values."
  [f & values]
  (fmap* f values))

(defn fmap-on
  "Apply the given function to the given, potentially partially resolved
   values once the given `predicate` is fulfilled. Note that the predicate
   has to have an arity matching the number of values."
  [predicate f & values]
  (chain/chain-when
    (vec values)
    (fn [values]
      (and (p/every-processable? values)
           (apply predicate values)))
    #(apply f %)))

(defn fmap!
  "Apply the given function to the given values once they are fully resolved."
  [f & values]
  (chain/chain-blocking
    (vec values)
    #(apply f %)))
