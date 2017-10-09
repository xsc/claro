(ns claro.runtime.impl.manifold
  (:require [manifold.deferred :as d]))

(def impl
  {:deferrable? d/deferrable?
   :deferred?   d/deferred?
   :->deferred  d/->deferred
   :value       d/success-deferred
   :chain       #(apply d/chain % %2)
   :catch       d/catch
   :zip         #(apply d/zip %)
   :run         #(d/future (%))
   :loop-fn     #(d/loop [state %2] (%1 state))
   :recur-fn    #(d/recur %)})
