(ns claro.engine.core
  (:require [claro.engine.runtime :as runtime]
            [claro.data.protocols :as p]
            [claro.data.tree :as tree]
            [manifold.deferred :as d]
            [potemkin :refer [defprotocol+]]))

;; ## Protocol

(defprotocol+ WrappableEngine
  "Protocol for a Resolution engine that supports wrapping of the
   batchwise-resolution fn."
  (wrap-resolver [engine wrap-fn]
    "Wrap the given engine's batchwise resolution fn using the given
     `wrap-fn`.")
  (wrap-selector [engine wrap-fn]
    "Wrap the given engine's selector function using the given `wrap-fn`."))

;; ## Engine

(defrecord Engine [opts]
  WrappableEngine
  (wrap-resolver [engine wrap-fn]
    (update-in engine [:opts :resolve-fn] wrap-fn))
  (wrap-selector [engine wrap-fn]
    (update-in engine [:opts :selector] #(wrap-fn (or % identity))))

  clojure.lang.IFn
  (invoke [_ resolvable]
    (runtime/run! opts (tree/wrap-tree resolvable))))

(alter-meta! #'map->Engine assoc :private true)
(alter-meta! #'->Engine assoc :private true)

;; ## Resolution Logic

(defn- resolve-them-all!
  [[head :as batch] env]
  (cond (instance? claro.data.protocols.BatchedResolvable head)
        (p/resolve-batch! head env batch)

        (next batch)
        (let [deferreds (mapv #(p/resolve! % env) batch)]
          (apply d/zip deferreds))

        :else
        (d/chain (p/resolve! head env) vector)))

;; ## Options

(defn- build-resolve-fn
  [{:keys [env] :or {env {}}}]
  (fn [batch]
    (d/chain
      (resolve-them-all! batch env)
      #(map tree/wrap-tree %))))

(defn- build-inspect-fn
  [_]
  #(p/resolvables %))

(defn- build-apply-fn
  [_]
  (fn [tree resolvable->value]
    (p/apply-resolved-values tree resolvable->value)))

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
