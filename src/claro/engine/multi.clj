(ns claro.engine.multi
  (:require [claro.engine.core :as engine]
            [claro.runtime.impl :as impl]))

;; ## Ordered Resolution

(defn- conj-and-recur
  [impl state result]
  (->> (-> state
           (update :result conj! result)
           (update :resolvables next))
       (impl/recur impl)))

(defn- resolve-next
  [{:keys [impl engine opts resolvables result] :as state}]
  (if-let [[r & rst] (seq resolvables)]
    (let [d (engine/run engine r opts)]
      (->> #(conj-and-recur impl state %)
           (impl/chain1 impl d)))
    (persistent! result)))

(defn- resolve-ordered!
  [engine resolvables opts]
  {:pre [(sequential? resolvables)]}
  (let [impl (engine/impl engine)]
    (cond (next resolvables)
          (->> {:impl        impl
                :engine      engine
                :opts        opts
                :resolvables resolvables,
                :result      (transient [])}
               (impl/loop impl resolve-next))

          (seq resolvables)
          (impl/chain1
            impl
            (engine/run engine (first resolvables) opts)
            vector)

          :else (impl/value impl []))))

;; ## Engine

(deftype MultiEngine [engine]
  engine/IEngine
  (wrap [_ wrap-fn]
    (MultiEngine. (engine/wrap engine wrap-fn)))
  (run [_ resolvables opts]
    (resolve-ordered! engine resolvables opts))
  (impl [_]
    (engine/impl engine))

  clojure.lang.IFn
  (invoke [this resolvable]
    (engine/run this resolvable {}))
  (invoke [this resolvable opts]
    (engine/run this resolvable opts)))

;; ## Builder

(defn build
  "Create a new engine targeting in-order resolution of resolvables."
  [engine]
  (->MultiEngine engine))
