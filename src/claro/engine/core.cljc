(ns claro.engine.core
  (:require [claro.engine.runtime :as runtime]
            [claro.data
             [tree :as tree]
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
  (fn [tree-resolvables]
    (let [resolvables (map tree/resolvable tree-resolvables)
          [resolvable :as batch] (distinct resolvables)]
      (d/chain
        (r/resolve-batch! resolvable env batch)
        (fn [resolved]
          (let [resolvable->resolved (zipmap batch resolved)]
            (map
              (fn [tree-resolvable]
                (->> (tree/resolvable tree-resolvable)
                     (resolvable->resolved)
                     (tree/set-resolved-value tree-resolvable)))
              tree-resolvables)))))))

(defn- build-inspect-fn
  [_]
  #(tree/tree-resolvables %))

(defn- build-apply-fn
  [_]
  (fn [value tree-resolvables]
    (tree/resolve-all value tree-resolvables)))

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
