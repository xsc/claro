(ns claro.data.ops.collections
  (:refer-clojure :exclude [drop first map nth take])
  (:require [claro.data.ops.chain :as chain]
            [claro.data.protocols :as p]
            [clojure.core :as core]))

;; ## Helper

(defn- every-processable?
  "Check whether every value in the given collection is processable."
  [sq]
  (every? p/processable? sq))

(defn- assert-coll
  [value msg pred]
  (assert (or (nil? value) (coll? value)) (str msg "\n" (pr-str value)))
  (when pred
    (assert (pred value) (str msg "\n" (pr-str value)))))

(defn- wrap-assert-coll
  ([f msg]
   (wrap-assert-coll f nil msg))
  ([f pred msg]
   (fn [value]
     (assert-coll value msg pred)
     (f value))))

;; ## Map

(defn- rechain-map
  [f values]
  (chain/rechain-when
    values
    every-processable?
    #(core/apply f %)))

(defn map
  "Iterate the given function over every element of the given, potentially
   partially resolved value."
  [f & sq]
  (let [rechain #(rechain-map f %&)]
    (chain/chain-when
      (vec sq)
      (wrap-assert-coll
        every-processable?
        "can only apply 'map' to collections, given:")
      #(core/apply core/map rechain %))))

;; ## Element Access

(defn first
  "Get the first element of the given resolvable."
  [value]
  (chain/chain-eager
    value
    (wrap-assert-coll
      core/first
      "can only apply 'first' to collections, given:")))

(defn nth
  "Get the nth element of the given resolvable."
  [value n]
  (chain/chain-when
    [value n]
    every-processable?
    (wrap-assert-coll
      (fn [[v n]]
        (core/nth v n))
      sequential?
      "can only apply 'nth' to sequentials, given:")))

;; ## Take/Drop

(defn take
  "Get first n elements of the given resolvable."
  [n value]
  (chain/chain-when
    [value n]
    every-processable?
    (wrap-assert-coll
      (fn [[v n]]
        (core/take n v))
      "can only apply 'take' to sequentials, given:")))

(defn drop
  "Drop first n elements of the given resolvable."
  [n value]
  (chain/chain-when
    [value n]
    every-processable?
    (wrap-assert-coll
      (fn [[v n]]
        (core/drop n v))
      "can only apply 'drop' to sequentials, given:")))
