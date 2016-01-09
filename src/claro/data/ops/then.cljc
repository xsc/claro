(ns claro.data.ops.then
  (:require [claro.data.ops.chain :as chain]))

(defn then
  "Wrap the given value with a processing function that gets called the moment
   the given value is neither a `Resolvable` nor wrapped."
  [value f & args]
  (chain/chain-eager value #(apply f % args)))

(defn then!
  "Wrap the given value with a processing function that gets called once the
   value has been fully resolved.

   Only use this for guaranteed finite expansion!"
  [value f & args]
  (chain/chain-blocking value #(apply f % args)))

(defn on
  "Wrap the given value with a processing function that gets called
   the moment the given value is neither a `Resolvable` nor wrapped,
   plus fulfills the given `predicate`."
  [value predicate f & args]
  (chain/chain-when value predicate #(apply f % args)))
