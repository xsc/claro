(ns claro.engine.middlewares.override
  (:require [claro.engine.protocols :as engine]
            [claro.runtime.impl :as impl]))

(defn override
  "Intercept resolution of the given class and return the value of the given
   single-arity function (called on the `Resolvable` value)."
  [engine resolvable-class single-resolve-fn]
  (->> (fn [resolver]
         (fn [env [resolvable :as batch]]
           (if (instance? resolvable-class resolvable)
             (let [impl (engine/impl engine)
                   result (mapv single-resolve-fn batch)]
               (if (impl/deferrable? impl result)
                 (impl/->deferred impl result)
                 (impl/chain1 impl result identity)))
             (resolver env batch))))
       (engine/wrap-resolver engine)))

(defn overrides
  "See `override`; intercepts resolution of all classes used as keys in the
   given map."
  [engine class->single-resolve-fn]
  (reduce
    (fn [engine [resolvable-class single-resolve-fn]]
      (override engine resolvable-class single-resolve-fn))
    engine class->single-resolve-fn))
