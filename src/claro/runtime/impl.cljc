(ns claro.runtime.impl
  (:refer-clojure :exclude [loop recur]))

;; ## Rationale
;;
;; The runtime should provide resolution capabilities independent of the
;; actual deferred implementation (manifold, core.async, promises). So,
;; we need to inject the following functionality:
;;
;; - `:deferrable?`: is the value compatible with the deferred impl,
;; - `->deferred`: convert a value to an instance of the deferred impl,
;; - `chain`: add postprocessing to a deferred,
;; - `zip`: concatenate results of multiple deferreds,
;; - `loop-fn`: run's a function as long as it returns a value created by `:recur-fn`,
;; - `recur-fn`: advices `:loop-fn` to continue the loop.
;;
;; This should enable usage of claro with any kind of deferred library, esp.
;; ClojureScript ones, without any further adjustments.

(defn ->deferred-impl
  [{:keys [deferrable?
           deferred?
           ->deferred
           value
           chain
           zip
           loop-fn
           recur-fn] :as impl}]
  {:pre [(fn? deferrable?)
         (fn? deferred?)
         (fn? ->deferred)
         (fn? value)
         (fn? chain)
         (fn? zip)
         (fn? loop-fn)
         (fn? recur-fn)]}
  impl)

(defn deferrable?
  [{:keys [deferrable?]} value]
  (deferrable? value))

(defn deferred?
  [{:keys [deferred?]} value]
  (deferred? value))

(defn ->deferred
  [{:keys [->deferred]} value]
  (->deferred value))

(defn value
  [{:keys [value]} v]
  (value v))

(defn chain
  [{:keys [chain]} value & fs]
  (chain value fs))

(defn chain1
  [{:keys [chain]} value f]
  (chain value [f]))

(defn zip
  [{:keys [zip]} deferreds]
  (zip deferreds))

(defn loop
  [{:keys [loop-fn]} f initial-state]
  (loop-fn f initial-state))

(defn recur
  [{:keys [recur-fn]} new-state]
  (recur-fn new-state))
