(ns claro.middleware.cache
  "A middleware to allow caching of resolution results."
  (:require [claro.data.protocols :as p]
            [claro.engine :as engine]
            [claro.runtime.impl :as impl]
            [claro.middleware
             [intercept :refer [wrap-intercept]]
             [observe :refer [wrap-observe*]]]
            [potemkin :refer [defprotocol+]]))

;; ## Protocol

(defprotocol+ ResolvableCache
  "Protocol for cache implementations for resolvables."
  (cache-get
    [cache env resolvables]
    "Lookup cached results for the given resolvables. This should return a map
     associating resolvables with their value.")
  (cache-put
    [cache env resolvable->result]
    "Put resolved values into the cache."))

;; ## Middleware

(defn- cache-writer
  [cache]
  (fn [env batch resolvable->result]
    (let [cacheable (->> (for [[resolvable result :as e] resolvable->result
                               :when (not (p/pure-resolvable? resolvable))]
                           e)
                         (into {}))]
      (when-not (empty? cacheable)
        (cache-put cache env cacheable)))))

(defn- cache-reader
  [cache]
  (fn [env batch]
    (cache-get cache env batch)))

(defn wrap-cache
  "Wrap the given engine to allow caching and cache lookups of resolvables via
   the given cache.

   ```clojure
   (defonce cache
     (redis-cache ...))

   (defonce engine
     (-> (engine/engine)
         (wrap-cache cache)))
   ```

   Note that `PureResolvable` values will never hit the cache."
  [engine cache]
  {:pre [(satisfies? ResolvableCache cache)]}
  (-> engine
      (wrap-observe*
        engine/wrap-pre-transform
        (cache-writer cache))
      (wrap-intercept
        (cache-reader cache))))
