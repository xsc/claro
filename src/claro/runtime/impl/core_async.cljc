(ns claro.runtime.impl.core-async
  (:require
    #?(:clj  [clojure.core.async :as async :refer [<!]]
       :cljs [cljs.core.async :refer [<!]]))
  #?(:cljs (:require-macros [cljs.core.async.macros :as async])))

;; ## Chain/Zip

(defn- channel?
  [value]
  (instance?
    #?(:clj  clojure.core.async.impl.channels.ManyToManyChannel
       :cljs cljs.core.async.impl.channels.ManyToManyChannel)
    value))

(defn- ->channel
  [value]
  (if (channel? value)
    value
    (async/go value)))

(defn- chain
  [ch fs]
  (if (seq fs)
    (let [[f & rst] fs]
      (async/go
        (let [next-ch (if (channel? ch)
                        (f (<! ch))
                        (f ch))]
          (<! (chain next-ch rst)))))
    (->channel ch)))

(defn- catch-exceptions
  [ch f]
  (throw
    (Exception.
      "'catch' not supported for core.async deferrables.")))

(defn- zip
  [chs]
  (async/go-loop
    [chs    (seq chs)
     result []]
    (if chs
      (let [value (<! (first chs))]
        (recur (next chs) (conj result value) ))
      result)))

;; ## Loop/Recur

(deftype Recur [state])

(defn- async-recur
  [state]
  (->Recur state))

(defn- async-loop
  [f initial-state]
  (async/go-loop
    [state initial-state]
    (let [step (f state)
          value (if (channel? step) (<! step) step)]
      (if (instance? Recur value)
        (recur (.-state value))
        value))))

;; ## Implementation

(def impl
  {:deferrable? (constantly true)
   :deferred?   channel?
   :->deferred  ->channel
   :value       #(async/go %)
   :chain       chain
   :catch       catch-exceptions
   :zip         zip
   :run         #(async/thread (%))
   :loop-fn     async-loop
   :recur-fn    async-recur})
