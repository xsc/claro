(ns claro.runtime
  (:require [claro.runtime
             [application :refer [apply-resolved-batches]]
             [caching :as caching]
             [impl :as impl]
             [inspection :refer [inspect-resolvables]]
             [mutation :refer [maybe-resolve-mutations!]]
             [resolution :refer [resolve-batches!]]
             [selection :refer [select-resolvable-batches]]])
  (:refer-clojure :exclude [run!]))

;; ## Depth Protection

(defn- assert-batch-count!
  [{:keys [max-batches] :or {max-batches 256}} batch-count]
  (when (some-> max-batches (< batch-count))
    (throw
      (IllegalStateException.
        (format "resolution has exceeded maximum batch count/depth: %d/%d"
                batch-count
                max-batches)))))

;; ## Runtime Logic

(defn- apply-and-recur!
  [{:keys [impl] :as opts}
   {:keys [value cache] :as state}
   new-batch-count
   resolvable->value]
  (let [value (apply-resolved-batches opts value resolvable->value)
        cache (caching/update-cache opts cache resolvable->value)]
    (impl/recur
      impl
      (assoc! state
              :value       value
              :cache       cache
              :batch-count new-batch-count))))

(defn- resolve-and-recur!
  [{:keys [impl] :as opts}
   state
   new-batch-count
   resolvable-deferred]
  (impl/chain1
    impl
    resolvable-deferred
    #(apply-and-recur! opts state new-batch-count %)))

(defn- run-step!
  [{:keys [impl] :as opts} {:keys [value cache batch-count] :as state}]
  (let [resolvables (inspect-resolvables opts value)]
    (if (empty? resolvables)
      value
      (or (some->> (maybe-resolve-mutations! opts state resolvables)
                   (resolve-and-recur! opts state (inc batch-count)))
          (let [batches (select-resolvable-batches opts resolvables)
                new-batch-count (+ batch-count (count batches))]
            (assert-batch-count! opts new-batch-count)
            (if (seq batches)
              (->> (resolve-batches! opts cache batches)
                   (resolve-and-recur! opts state new-batch-count))
              value))))))

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
  {:pre [(every? fn? (map opts [:inspect-fn :resolve-fn :apply-fn]))]}
  (impl/loop
    impl
    #(run-step! opts %)
    (transient
      {:value value
       :batch-count 0
       :cache (caching/init-cache opts)})))
