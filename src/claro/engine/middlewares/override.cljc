(ns claro.engine.middlewares.override
  (:require [claro.engine.protocols :as engine]
            [claro.runtime.impl :as impl]))

(defn override
  [engine resolvable-class single-resolve-fn]
  (->> (fn [resolver]
         (fn [[resolvable :as batch]]
           (if (instance? resolvable-class resolvable)
             (let [impl (engine/impl engine)
                   result (mapv single-resolve-fn batch)]
               (if (impl/deferrable? impl result)
                 (impl/->deferred impl result)
                 (impl/chain1 impl result identity)))
             (resolver batch))))
       (engine/wrap-resolver engine)))
