(ns claro.middleware.deferred
  "Generic middlewares to adjust the resolution deferred."
  (:require [claro.engine.core :as engine]
            [claro.runtime.impl :as impl]))

(defn wrap-deferred
  "Middleware that will call `f` on any batchwise-resolution deferred value,
   if the batch of `Resolvable` values matches `predicate`. For example, to
   set a timeout on `Person` batch resolution (assuming Manifold deferreds):

   ```clojure
   (def run-engine
     (-> (engine/engine)
         (wrap-deferred
           #(instance? Person (first %))
           (fn [env deferred]
             (d/timeout! deferred (:timeout env 1000))))))
   ```

   > __Note:__ `f` will be called on the environment and the batch.

   If no `predicate` is given `f` will be called on all deferred values."
  ([engine f]
   (wrap-deferred engine (constantly true) f))
  ([engine predicate f]
   {:pre [(ifn? predicate) (ifn? f)]}
   (->> (fn [resolver]
          (fn [env batch]
            (cond->> (resolver env batch)
              (predicate batch) (f env))))
        (engine/wrap engine))))
