(ns claro.runtime.caching
  (:require [claro.runtime.impl :as impl]))

(defn init-cache
  "Initialize the cache."
  [_]
  (transient {}))

(defn update-cache
  "Update the cache."
  [_ cache resolvable->value]
  (reduce
    #(assoc! % (key %2) (val %2))
    cache
    resolvable->value))

(defn read-cache
  [_ cache resolvable not-found]
  (get cache resolvable not-found))
