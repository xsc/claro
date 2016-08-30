(ns claro.runtime.resolution
  (:require [claro.runtime.impl :as impl]
            [claro.runtime.caching :as caching]))

(defn- assert-deferrable
  "Make sure the given value is a deferrable, throw `IllegalStateException`
   otherwise."
  [{:keys [impl]} batch value]
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
  [{:keys [resolve-fn env impl] :as opts} batch]
  (some->> batch
           (resolve-fn env)
           (assert-deferrable opts batch)
           (impl/->deferred impl)))

(defn- assert-resolution-count!
  [batch resolved-values]
  (let [batch-count (count batch)
        resolved-count (count resolved-values)]
    (when (< resolved-count batch-count)
      (throw
        (IllegalStateException.
          (str "some of the values in the current batch were not resolved – "
               (if (= batch-count 1)
                 "1 value was"
                 (str batch-count " values were"))
               " given, only "
               (if (= resolved-count 1)
                 "1 was"
                 (str resolved-count " were"))
               " produced.\nin:  "
               (pr-str (vec batch)) "\nout: "
               (pr-str (vec resolved-values))))))))

(defn- merge-resolvables
  "Merge all resolved values with the original batch."
  [batch resolved-values]
  (assert-resolution-count! batch resolved-values)
  (zipmap batch resolved-values))

(defn- resolve-batch!
  "Returns a deferred representing the resolution of the given batch.
   `resolve-fn` has to return a deferred with the resolution results
   in-order."
  [{:keys [impl] :as opts} batch]
  (impl/chain1
    impl
    (generate-deferred opts batch)
    #(merge-resolvables batch %)))

(defn- resolve-batches-with-cache-step!
  [opts cache result batch]
  (loop [batch  batch
         result result
         uncached (transient [])]
    (if (seq batch)
      (let [[h & rst] batch
            v (caching/read-cache opts cache h ::miss)]
        (if (not= v ::miss)
          (recur rst (assoc-in result [:cached h] v) uncached)
          (recur rst result (conj! uncached h))))
      (if-let [rs (seq (persistent! uncached))]
        (update result :ds conj (resolve-batch! opts rs))
        result))))

(defn- resolve-batches-with-cache!
  [opts cache batches]
  (reduce
    #(resolve-batches-with-cache-step! opts cache %1 %2)
    {:cached {}, :ds []}
    batches))

(defn resolve-batches!
  "Resolve the given batches, returning a deferred with a map of
   original value -> resolved value pairs."
  [{:keys [impl] :as opts} cache batches]
  (let [{:keys [cached ds]} (resolve-batches-with-cache! opts cache batches)
        zipped (impl/zip impl ds)]
    (impl/chain1 impl zipped #(into cached %))))
