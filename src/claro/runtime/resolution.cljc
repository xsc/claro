(ns claro.runtime.resolution
  (:require [claro.runtime.impl :as impl]
            [claro.runtime.state :as state]))

(defn- assert-deferrable
  "Make sure the given value is a deferrable, throw `IllegalStateException`
   otherwise."
  [impl batch value]
  (when-not (impl/deferrable? impl value)
    (throw
      (IllegalStateException.
        (str "'resolve-fn' has to return a deferrable for class "
             (-> batch first class (.getName))
             ", returned:"
             (-> value class (.getName))))))
  value)

(defn- generate-deferred
  "Create a function that takes batch of resolvables and generates a deferred
   containing the in-order results."
  [state batch]
  (let [resolve-fn (state/opt state :resolve-fn)
        env        (state/opt state :env {})
        impl       (state/impl state)]
    (some->> batch
             (resolve-fn env)
             (assert-deferrable impl batch)
             (impl/->deferred impl))))

(defn- assert-every-resolution!
  [batch resolved-values]
  {:pre [(map? resolved-values)]}
  (let [missing (keep
                  #(when (not (contains? resolved-values %))
                     %)
                  batch)]
    (when (seq missing)
      (throw
        (IllegalStateException.
          (str "some of the values in the current batch were not resolved.\n"
               "missing: " (pr-str (vec missing)) "\n"
               "in:      " (pr-str (vec batch)) "\n"
               "out:     " (pr-str resolved-values))))))
  resolved-values)

(defn- resolve-batch!
  "Returns a deferred representing the resolution of the given batch.
   `resolve-fn` has to return a deferred with the resolution results
   in-order."
  [state batch]
  (impl/chain1
    (state/impl state)
    (generate-deferred state batch)
    #(assert-every-resolution! batch %)))

(defn- resolve-batches-with-cache-step!
  [state result batch]
  (loop [batch  batch
         result result
         uncached (transient [])]
    (if (seq batch)
      (let [[h & rst] batch
            v (state/from-cache state h ::miss)]
        (if (not= v ::miss)
          (recur rst (assoc-in result [:cached h] v) uncached)
          (recur rst result (conj! uncached h))))
      (if-let [rs (seq (persistent! uncached))]
        (update result :ds conj (resolve-batch! state rs))
        result))))

(defn- resolve-batches-with-cache!
  [state]
  (reduce
    #(resolve-batches-with-cache-step! state %1 %2)
    {:cached {}, :ds []}
    (state/batches state)))

(defn resolve-batches!
  "Resolve the given batches, returning a deferred with a map of
   original value -> resolved value pairs."
  [state]
  (let [impl                (state/impl state)
        {:keys [cached ds]} (resolve-batches-with-cache! state)
        zipped              (impl/zip impl ds)]
    (impl/chain1 impl zipped #(into cached %))))
