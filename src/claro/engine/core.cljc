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
    "Wrap the engine's complete resolver function using the given `wrap-fn`.
     The resolution results will be wrapped as trees. See [[wrap-transform]]
     and [[wrap-pre-transform]] for access to the transformed and raw
     results.")
  (^{:added "0.2.8"} wrap-transform [engine wrap-fn]
    "Wrap the given engine's resolver (after transformation but before tree
     wrapping) using the given `wrap-fn`.")
  (^{:added "0.2.8"} wrap-pre-transform [engine wrap-fn]
    "Wrap the given engine's resolver (before transformation) using the given
     `wrap-fn`.")
  (run [engine resolvable opts]
    "Resolve the given `resolvable` using the given engine, using `opts` to
     override per-run options:

     - `:env`
     - `:selector`

     Will return a deferred value.")
  (impl [engine]
    "Return the given engines deferred implementation."))

;; ## Type

(defn- attach-resolve-fn
  [{:keys [wrap-transform wrap-finalize raw-resolve-fn] :as opts}]
  (->> (concat wrap-transform wrap-finalize)
       (resolver/build raw-resolve-fn)
       (assoc opts :resolve-fn)))

(defn- run-via-runtime!
  [opts selector resolvable]
  (runtime/run!
    (assoc opts :select-fn (selector/instantiate selector))
    (wrap-tree resolvable)))

(defn- wrap*
  [opts wrap-fn]
  (-> opts
      (update :wrap-finalize conj wrap-fn)
      (attach-resolve-fn)))

(defn- wrap-transform*
  [opts wrap-fn]
  (-> opts
      (update :wrap-transform conj wrap-fn)
      (attach-resolve-fn)))

(defn- wrap-pre-transform*
  [opts wrap-fn]
  (-> opts
      (update :wrap-transform #(into [wrap-fn] %))
      (attach-resolve-fn)))

(deftype Engine [selector opts]
  IEngine
  (wrap [engine wrap-fn]
    (->> (wrap* opts wrap-fn)
         (Engine. selector)))
  (wrap-transform [engine wrap-fn]
    (->> (wrap-transform* opts wrap-fn)
         (Engine. selector)))
  (wrap-pre-transform [engine wrap-fn]
    (->> (wrap-pre-transform* opts wrap-fn)
         (Engine. selector)))
  (impl [_]
    (:impl opts))
  (run [_ resolvable {env' :env, selector' :selector}]
    (run-via-runtime!
      (-> opts
          (update :env merge env'))
      (or selector' selector)
      resolvable))

  clojure.lang.IFn
  (invoke [this resolvable]
    (run-via-runtime! opts selector resolvable))
  (invoke [this resolvable opts]
    (run this resolvable opts)))

(alter-meta! #'->Engine assoc :private true)

;; ## Options

(def ^:private fixed-runtime-opts
  {:mutation?    p/mutation?
   :cost-fn      #(p/cost (first %) %)
   :inspect-fn   p/resolvables
   :partition-fn p/partition-batch
   :apply-fn     p/apply-resolved-values})

(defn- ->runtime-opts
  [impl {:keys [adapter] :as opts}]
  (let [impl (impl/->deferred-impl impl)]
    (-> opts
        (merge fixed-runtime-opts)
        (dissoc :selector)
        (assoc :impl           impl
               :wrap-finalize  [#(resolver/wrap-finalize impl %)]
               :wrap-transform [#(resolver/wrap-transform impl %)]
               :raw-resolve-fn (resolver/raw-resolve-fn impl adapter))
        (attach-resolve-fn))))

;; ## Builder

(defn build
  "Create a new Engine relying on the given deferred implementation and
   options."
  [impl {:keys [selector] :as opts}]
  {:pre [selector]}
  (->Engine selector (->runtime-opts impl opts)))
