(ns claro.engine.builder
  (:require [claro.engine.protocols :as p]
            [claro.runtime :as runtime]
            [claro.runtime.impl :as impl]
            [claro.data
             [protocols :as data]
             [tree :refer [wrap-tree]]]))

;; ## Implementation

(deftype Engine [opts]
  p/Engine
  (wrap-resolver [engine wrap-fn]
    (Engine. (update opts :resolve-fn wrap-fn)))
  (wrap-selector [engine wrap-fn]
    (Engine. (update opts :selector #(wrap-fn (or % identity)))))
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

;; ## Builders

;; ### Resolution

(defn- resolve-them-all!
  [{:keys [impl]} env [head :as batch]]
  (cond (data/batched-resolvable? head)
        (data/resolve-batch! head env batch)

        (next batch)
        (let [deferreds (mapv #(data/resolve! % env) batch)]
          (impl/zip impl deferreds))

        :else
        (impl/chain1 impl (data/resolve! head env) vector)))

(defn- build-resolve-fn
  [{:keys [impl] :as opts}]
  (fn [env batch]
    (impl/chain
      impl
      (resolve-them-all! opts env batch)
      #(map wrap-tree %))))

;; ### Inspection

(defn- build-inspect-fn
  [_]
  #(data/resolvables %))

;; ### Application

(defn- build-apply-fn
  [_]
  (fn [tree resolvable->value]
    (data/apply-resolved-values tree resolvable->value)))

;; ## Options

(defn- engine-opts
  [impl opts]
  (let [opts' (assoc opts :impl (impl/->deferred-impl impl))]
    (merge
      opts'
      {:resolve-fn (build-resolve-fn opts')
       :inspect-fn (build-inspect-fn opts')
       :apply-fn   (build-apply-fn opts')})))

;; ## Builder Function

(defn build
  "Create a new Engine relying on the given deferred implementation and
   options."
  [impl opts]
  (->Engine (engine-opts impl opts)))
