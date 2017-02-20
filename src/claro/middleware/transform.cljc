(ns claro.middleware.transform
  "Generic middlewares to transform resolution results."
  (:require [claro.engine.core :as engine]
            [claro.runtime.impl :as impl]))

;; ## Transform Logic

(defn- transform!
  [predicate transform-fn result]
  (->> (for [[resolvable resolved] result]
         [resolvable (if (predicate resolvable)
                       (transform-fn resolvable resolved)
                       resolved)])
       (into {})))

;; ## Middlewares

(defn wrap-transform
  "Middleware that will pass any `Resolvable` that matches `Predicate` – as
   well as the resolved result – to `transform-fn` and use its return value in
   place of the actual result.

   For example, to inject the current timestamp into each `Person` record:

   ```clojure
   (def run-engine
     (-> (engine/engine)
         (wrap-transform
           #(instance? Person %)
           #(assoc %2 :__timestamp (System/currentTimeMillis)))))
   ```

   If no `predicate` is given, all `Resolvable` values will be transformed."
  ([engine transform-fn]
   (wrap-transform engine (constantly true) transform-fn))
  ([engine predicate transform-fn]
   {:pre [(ifn? predicate) (ifn? transform-fn)]}
   (->> (fn [resolver]
          (fn [env batch]
            (impl/chain1
              (engine/impl engine)
              (resolver env batch)
              #(transform! predicate transform-fn %))))
        (engine/wrap-transform engine))))

(defn wrap-transform-by-class
  "Middleware that will pass any `Resolvable` of one of the given
   `classes-to-transform` – as well as the resolved result – to `transform-fn`
   and use its return value in place of the actual result.

   For example, to inject the current timestamp into each `Person` record:

   ```clojure
   (def run-engine
     (-> (engine/engine)
         (wrap-transform-classes
           [Person]
           #(assoc %2 :__timestamp (System/currentTimeMillis)))))
   ```
   "
  [engine classes-to-transform transformer-fn]
  {:pre [(seq classes-to-transform)]}
  (let [predicate (fn [resolvable]
                    (some
                      #(instance? % resolvable)
                      classes-to-transform))]
    (wrap-transform engine predicate transformer-fn)))
