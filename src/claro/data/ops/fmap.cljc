(ns claro.data.ops.fmap
  (:require [claro.data.ops.chain :as chain]))

(defn fmap*
  "Apply the given function to the given, potentially partially resolved
   seq of values."
  [f values]
  (chain/chain-when
    (vec values)
    chain/every-processable?
    #(apply f %)))

(defn fmap
  "Apply the given function to the given, potentially partially resolved
   values."
  [f & values]
  (fmap* f values))

(defn fmap!
  "Apply the given function to the given values once they are fully resolved."
  [f & values]
  (chain/chain-blocking
    (vec values)
    #(apply f %)))
