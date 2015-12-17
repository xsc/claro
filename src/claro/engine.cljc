(ns claro.engine
  (:require [claro.engine.core :as engine]
            [claro.engine.middlewares
             [override :as override]
             [trace :as trace]]
            [potemkin :refer [import-vars]]))

;; ## Constructor

(defn engine
  ([] (engine {}))
  ([opts] (engine/create opts)))

;; ## Middlewares

(import-vars
  [claro.engine.core
   wrap]
  [claro.engine.middlewares.override
   override]
  [claro.engine.middlewares.trace
   tracing])
