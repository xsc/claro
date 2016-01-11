(ns claro.data.ops.collections
  (:refer-clojure :exclude [map])
  (:require [claro.data.ops.chain :as chain]
            [claro.data.protocols :as p]
            [clojure.core :as core]))

;; ## Helper

(defn- every-processable?
  "Check whether every value in the given collection is processable."
  [sq]
  (every? p/processable? sq))

;; ## Map

(defn- rechain-map
  [f values]
  (chain/rechain-when
    values
    every-processable?
    #(apply f %)))

(defn map
  "Iterate the given function over every element of the given, potentially
   partially resolved value."
  [f & sq]
  (let [rechain #(rechain-map f %&)]
    (chain/chain-when
      (vec sq)
      every-processable?
      #(apply core/map rechain %))))
