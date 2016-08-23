(ns claro.engine.core
  (:require [claro.runtime :as runtime]
            [claro.runtime.impl :as impl]
            [claro.data
             [protocols :as p]
             [tree :refer [wrap-tree]]]
            [claro.engine.resolver :as resolver]
            [potemkin :refer [defprotocol+]]))

;; ## Protocol

(defprotocol+ IEngine
  "Protocol for a Resolution engine that supports wrapping of the
   resolver fn."
  (wrap-resolver [engine wrap-fn]
    "Wrap the given engine's batchwise resolution fn using the given
     `wrap-fn`.")
  (impl [engine]
    "Return the given engines deferred implementation."))

;; ## Type

(deftype Engine [opts]
  IEngine
  (wrap-resolver [engine wrap-fn]
    (Engine. (update opts :resolve-fn wrap-fn)))
  (impl [_]
    (:impl opts))

  clojure.lang.IFn
  (invoke [_ resolvable]
    (runtime/run! opts (wrap-tree resolvable)))
  (invoke [_ resolvable {:keys [env]}]
    (runtime/run!
      (update opts :env merge env)
      (wrap-tree resolvable))))

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
        (assoc :impl       impl
               :resolve-fn (resolver/build impl adapter)))))

;; ## Builder

(defn build
  "Create a new Engine relying on the given deferred implementation and
   options."
  [impl opts]
  (->Engine (->runtime-opts impl opts)))
