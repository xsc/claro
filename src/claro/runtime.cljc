(ns claro.runtime
  (:require [claro.runtime
             [application :refer [apply-resolved-batches]]
             [impl :as impl]
             [inspection :refer [inspect-resolvables]]
             [mutation :refer [select-mutation-batches]]
             [resolution :refer [resolve-batches!]]
             [selection :refer [select-resolvable-batches]]
             [state :as state]])
  (:refer-clojure :exclude [run!]))

(defn- resolve-batches-in-state
  [state]
  (let [impl             (state/impl state)
        resolve-deferred (resolve-batches! state)]
    (->> (fn [resolvable->value]
           (let [value (apply-resolved-batches state resolvable->value)]
             (state/finalize state resolvable->value value)))
         (impl/chain1 impl resolve-deferred))))

(defn run-step
  "Run a single resolution step. Return a deferred value with the
   state after resolution."
  [state]
  (let [resolvables (inspect-resolvables state)]
    (if-not (empty? resolvables)
      (let [batches (or (select-mutation-batches state resolvables)
                        (select-resolvable-batches state resolvables))]
        (if (seq batches)
          (-> state
              (state/set-batches batches)
              (resolve-batches-in-state))
          (state/done state)))
      (state/done state))))

(defn- run-step-and-recur
  "Run a single resolution step, return 'recur' value if resolution is not
   done."
  [state]
  (let [impl (state/impl state)
        state-deferred (run-step state)]
    (->> (fn [state']
           (if-not (state/done? state')
             (impl/recur impl state')
             state'))
         (impl/chain1 impl state-deferred))))

(defn run!*
  "Like [[run!]] but will produce the complete resolution state."
  [{:keys [impl] :as opts} value]
  {:pre [(every?
           (comp fn? opts)
           [:select-fn :inspect-fn :resolve-fn :apply-fn])]}
  (->> (state/initialize opts value)
       (impl/loop impl run-step-and-recur)))

(defn run!
  "Run the resolution engine on the given value. `opts` is a map of:

   - `:inspect-fn`: a function that, given a value, returns a seq of all
      available resolvables within that value,
   - `:select-fn`: a function that, given a seq of resolvable classes returns
     those to resolve during the next step,
   - `:mutation?`: a function that, given a seq of resolvables, returns whether
     or not said resolvable represents a mutation,
   - `:resolve-fn`: a function that given a seq of resolvables of the same class
     returns a manifold deferred with resolved values in-order,
   - `:apply-fn`: a function that takes the original value, as well as a map
     of resolvable -> resolved value pairs, and returns a map of `:value` and
     `:resolvables`, where `:value` is the now-more-resolved value for the next
     iteration and `:resolvables` the new resolvables within,
   - `:max-batches`: an integer describing the maximum number of batches to
     resolve before throwing an `IllegalStateException`.

   Returns a manifold deferred with the resolved result."
  [{:keys [impl] :as opts} value]
  (impl/chain1
    impl
    (run!* opts value)
    :value))
