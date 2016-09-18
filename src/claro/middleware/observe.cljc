(ns claro.middleware.observe
  "Generic middlewares to observe resolution results."
  (:require [claro.engine.core :as engine]
            [claro.runtime.impl :as impl]))

;; ## Observe Logic

(defn- observe!
  [predicate observer-fn result]
  (doseq [[resolvable resolved] result
          :when (predicate resolvable)]
    (observer-fn resolvable resolved))
  result)

;; ## Middlewares

(defn wrap-observe
  "Middleware that will pass any `Resolvable` that matches `predicate` – as well
   as the resolved result – to `observer-fn`.

   For example, to check the result of a specific `Person` record:

   ```clojure
   (def run-engine
     (-> (engine/engine)
         (wrap-observe
           #(and (instance? Person %) (= (:id %) 1))
           prn)))
   ```

   If no `predicate` is given, all `Resolvable` values will be observed."
  ([engine observer-fn]
   (wrap-observe engine (constantly true) observer-fn))
  ([engine predicate observer-fn]
   {:pre [(ifn? predicate) (ifn? observer-fn)]}
   (->> (fn [resolver]
          (fn [env batch]
            (impl/chain1
              (engine/impl engine)
              (resolver env batch)
              #(observe! predicate observer-fn %))))
        (engine/wrap engine))))

(defn wrap-observe-classes
  "Middleware that will pass any `Resolvable` of one of the given
   `classes-to-observe` – as well as the resolved result – to `observer-fn`.

   For example, to check the result of a specific `Person` record:

   ```clojure
   (def run-engine
     (-> (engine/engine)
         (wrap-observe-classes [Person] prn)))
   ```
   "
  [engine classes-to-observe observer-fn]
  {:pre [(seq classes-to-observe)]}
  (let [predicate (fn [resolvable]
                    (some
                      #(instance? % resolvable)
                      classes-to-observe))]
    (wrap-observe engine predicate observer-fn)))
