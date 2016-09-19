(ns claro.engine.adapter
  "Adapters can be used to remove asynchronous execution boilerplate or mediate
   between differen deferred implementations."
  (:require [claro.runtime.impl :as impl]))

(defn default-adapter
  "This adapter expects `Resolvable` values to already return a deferrable
   value."
  [_ f]
  (f))

(defn sync-adapter
  "This adapter expects `Resolvable` values to resolve synchronously, wrapping
   them in asynchronous processing according to the given implementation."
  [impl f]
  (impl/run impl f))
