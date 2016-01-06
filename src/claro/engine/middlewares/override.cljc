(ns claro.engine.middlewares.override
  (:require [claro.engine.protocols :as engine]
            [claro.runtime.impl :as impl]))

(defn override
  [engine resolvable-class single-resolve-fn]
  (->> (fn [resolver]
         (fn [[resolvable :as batch]]
           (if (instance? resolvable-class resolvable)
             (impl/->deferred
               (engine/impl engine)
               (mapv single-resolve-fn batch))
             (resolver batch))))
       (engine/wrap-resolver engine)))
