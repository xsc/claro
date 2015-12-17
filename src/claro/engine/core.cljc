(ns claro.engine.core
  (:require [claro.engine.runtime :as runtime]
            [claro.data
             [resolvable-wrapper :as w]
             [resolvable :as r]]
            [manifold.deferred :as d]
            [potemkin :refer [defprotocol+]]))

;; ## Protocol

(defprotocol+ WrappableEngine
  "Protocol for a Resolution engine that supports wrapping of the
   batchwise-resolution fn."
  (wrap [engine wrap-fn]
    "Wrap the given engine's batchwise resolution fn using the given
     `wrap-fn`."))

;; ## Engine

(defrecord Engine [opts]
  WrappableEngine
  (wrap [engine wrap-fn]
    (update-in engine [:opts :resolve-fn] wrap-fn))

  clojure.lang.IFn
  (invoke [_ resolvable]
    (runtime/run! opts resolvable)))

(alter-meta! #'map->Engine assoc :private true)
(alter-meta! #'->Engine assoc :private true)

;; ## Options

(defn- build-resolve-fn
  [{:keys [env] :or {env {}}}]
  (fn [[resolvable :as batch]]
    (r/resolve-batch! resolvable env batch)))

(defn- build-inspect-fn
  [_]
  #(w/resolvables %))

(defn- build-apply-fn
  [_]
  #(w/apply-resolved %1 %2))

(defn- engine-opts
  [opts]
  (merge
    opts
    {:resolve-fn (build-resolve-fn opts)
     :inspect-fn (build-inspect-fn opts)
     :apply-fn   (build-apply-fn opts)}))

;; ## Constructor

(defn create
  "Create a new Engine."
  [opts]
  (->Engine (engine-opts opts)))
