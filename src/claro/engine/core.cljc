(ns claro.engine.core
  (:require [claro.runtime :as runtime]
            [claro.runtime.impl :as impl]
            [claro.data
             [protocols :as p]
             [tree :refer [wrap-tree]]]
            [claro.engine.resolver :as resolver]
            [claro.engine.selector :as selector]
            [potemkin :refer [defprotocol+]]))

;; ## Protocol

(defprotocol+ IEngine
  "Protocol for a Resolution engine that supports wrapping of the
   resolver fn."
  (wrap [engine wrap-fn]
    "Wrap the given engine's resolver using the given `wrap-fn`.")
  (impl [engine]
    "Return the given engines deferred implementation."))

;; ## Type

(defn- run-via-runtime!
  [opts selector resolvable]
  (runtime/run!
    (assoc opts :select-fn (selector/instantiate selector))
    (wrap-tree resolvable)))

(deftype Engine [selector opts]
  IEngine
  (wrap [engine wrap-fn]
    (Engine. selector (update opts :resolve-fn wrap-fn)))
  (impl [_]
    (:impl opts))

  clojure.lang.IFn
  (invoke [this resolvable]
    (run-via-runtime! opts selector resolvable))
  (invoke [_ resolvable {env' :env, selector' :selector}]
    (run-via-runtime!
      (-> opts
          (update :env merge env'))
      (or selector' selector)
      resolvable)))

(alter-meta! #'->Engine assoc :private true)

;; ## Options

(def ^:private fixed-runtime-opts
  {:mutation?  p/mutation?
   :inspect-fn p/resolvables
   :apply-fn   p/apply-resolved-values})

(defn- ->runtime-opts
  [impl {:keys [adapter] :as opts}]
  (let [impl (impl/->deferred-impl impl)]
    (-> opts
        (merge fixed-runtime-opts)
        (dissoc :selector)
        (assoc :impl       impl
               :resolve-fn (resolver/build impl adapter)))))

;; ## Builder

(defn build
  "Create a new Engine relying on the given deferred implementation and
   options."
  [impl {:keys [selector] :as opts}]
  {:pre [selector]}
  (->Engine selector (->runtime-opts impl opts)))
