(ns claro.engine.middlewares.override
  (:require [claro.engine.core :as engine]
            [manifold.deferred :as d]))

(defn override
  [engine resolvable-class single-resolve-fn]
  (->> (fn [resolver]
         (fn [[resolvable :as batch]]
           (if (instance? resolvable-class resolvable)
             (d/success-deferred
               (mapv single-resolve-fn batch))
             (resolver batch))))
       (engine/wrap-resolver engine)))
