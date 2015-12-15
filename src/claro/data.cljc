(ns claro.data
  (:require [claro.data
             [resolvable :as r]
             [resolvable-wrapper :as w]
             [engine :as engine]
             composition
             projection]
            [potemkin :refer [import-vars]]))

;; ## Engine

(defn- build-resolve-fn
  [{:keys [env overrides wrap-resolve]
    :or {env {}, wrap-resolve identity}
    :as opts}]
  (wrap-resolve
    (fn [[resolvable :as batch]]
      (if-let [override (get overrides (class resolvable))]
        (override env batch)
        (r/resolve-batch! resolvable env batch)))))

(defn- build-inspect-fn
  [{:keys [wrap-inspect]
    :or {wrap-inspect identity}}]
  (wrap-inspect
    #(w/resolvables %)))

(defn- build-apply-fn
  [{:keys [wrap-apply]
    :or {wrap-apply identity}}]
  (wrap-apply
    #(w/apply-resolved %1 %2)))

(defn- engine-opts
  [opts]
  (merge
    {:resolve-fn (build-resolve-fn opts)
     :inspect-fn (build-inspect-fn opts)
     :apply-fn   (build-apply-fn opts)}
    (select-keys opts [:select-fn :deferred-fn])))

(defn engine
  "Build a resolution engine for `Resolvables` and `BatchedResolvables`."
  ([] (engine {}))
  ([opts]
   (let [opts (engine-opts opts)]
     #(engine/run! opts %))))

;; ## Facade

(import-vars
  [claro.data.composition
   chain-when
   chain-when-contains
   chain-keys
   chain-select-keys
   chain]

  [claro.data.projection
   project]

  [claro.data.resolvable
   Resolvable
   BatchedResolvable
   resolvable?
   resolve-if-possible!
   resolve!
   resolve-batch!]

  [claro.data.resolvable-wrapper
   resolvables
   apply-resolved])
