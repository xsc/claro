(ns claro.engine.runtime
  (:refer-clojure :exclude [run!])
  (:require [manifold.deferred :as d]))

;; ## Selection

(defn- assert-class-selected!
  "Make sure at least a single class was selected, otherwise throw
   an `IllegalStateException`."
  [classes]
  (when (empty? classes)
    (throw
      (IllegalStateException.
        "resolvables were available but 'select-fn' did not choose any.")))
  classes)

(defn- assert-classes-valid!
  "Make sure all selected classes were actually candidates, otherwise
   throw an `IllegalStateException`."
  [resolvables classes]
  (doseq [class classes]
    (when-not (contains? resolvables class)
      (throw
        (IllegalStateException.
          (str "'select-fn' chose an unknown resolvable class:" class)))))
  classes)

(defn- select-resolvable-batches
  "Use the given `select-fn` (seq of classes -> seq of classes) and
   `inspect-fn` (value -> seq of resolvables) to collect batches of
   resolvables. Returns a seq of such batches."
  [{:keys [select-fn inspect-fn]
    :or {select-fn identity}} value]
  {:pre [(fn? inspect-fn)]}
  (let [resolvables (group-by class (inspect-fn value))]
    (some->> (seq (keys resolvables))
             (select-fn)
             (assert-class-selected!)
             (assert-classes-valid! resolvables)
             (mapv #(get resolvables %)))))

;; ## Resolution

(defn- assert-deferrable
  "Make sure the given value is a manifold deferrable, throw
   `IllegalStateException` otherwise."
  [batch value]
  (when-not (d/deferrable? value)
    (throw
      (IllegalStateException.
        (str "'resolve-fn' has to return a manifold deferrable for class "
             (-> batch first class (.getName))
             ", returned:"
             (-> value class (.getName))))))
  value)

(defn- generate-deferred
  "Create a function that takes batch of resolvables and generates a manifold
   deferred."
  [{:keys [resolve-fn deferred-fn]
    :or {deferred-fn (fn [_ d] d)}} batch]
  (let [assertion #(assert-deferrable batch %)]
    (comp #(deferred-fn batch %)
          d/->deferred
          assertion
          resolve-fn)))

(defn- merge-resolvables
  "Merge all resolved values with the original batch."
  [batch resolved-values]
  (let [batch-count (count batch)
        resolved-values (take batch-count resolved-values)]
    (when (< batch-count (count resolved-values))
      (throw
        (IllegalStateException.
          (str "'resolve-fn' did not resolve all values - "
               (count batch) " values were given, only "
               (count resolved-values) " were produced."))))
    (zipmap batch resolved-values)))

(defn- resolve-batch!
  "Returns a manifold deferred representing the resolution of the given batch.
   `resolve-fn` has to return a manifold deferred with the resolution results
   in-order."
  [opts batch]
  (d/chain
    batch
    (generate-deferred opts batch)
    #(merge-resolvables batch %)))

(defn- resolve-batches!
  "Resolve the given batches, returning a manifold deferred with a map of
   original value -> resolved value pairs."
  [opts batches]
  (let [ds (mapv #(resolve-batch! opts %) batches)]
    (d/chain
      (apply d/zip ds)
      #(into {} %))))

;; ## Application

(defn- apply-resolved-batches
  [{:keys [apply-fn]} value resolved-values]
  (apply-fn value resolved-values))

;; ## Engine

(defn- assert-batch-count!
  [{:keys [max-batches] :or {max-batches 256}} batch-count]
  (when (some-> max-batches (< batch-count))
    (throw
      (IllegalStateException.
        (format "resolution has exceeded maximum batch count/depth: %d/%d"
                batch-count
                max-batches)))))

(defn run!
  "Run the resolution engine on the given value. `opts` is a map of:

   - `:inspect-fn`: a function that, given a value, returns a seq of all
      available resolvables within that value,
   - `:select-fn`: a function that, given a seq of resolvable classes returns
     those to resolve during the next step,
   - `:resolve-fn`: a function that given a seq of resolvables of the same class
     returns a manifold deferred with resolved values in-order,
   - `:deferred-fn`: a function called on each manifold deferred encapsuling a
     single resolution step (parameters are batch-to-be-resolved, as well as
     the deferred value),
   - `:apply-fn`: a function that takes the original value, as well as a map
     of resolvable -> resolved value pairs, and unifies them into a
     now-more-resolved value for the next iteration,
   - `:max-batches`: an integer describing the maximum number of batches to
     resolve before throwing an `IllegalStateException`.

   Returns a manifold deferred with the resolved result."
  [opts value]
  {:pre [(every? fn? (map opts [:inspect-fn :resolve-fn :apply-fn]))]}
  (d/loop [value       value
           batch-count 0]
    (assert-batch-count! opts batch-count)
    (d/let-flow [value   value
                 batches (select-resolvable-batches opts value)]
      (if (seq batches)
        (d/chain
          batches
          #(resolve-batches! opts %)
          #(apply-resolved-batches opts value %)
          #(d/recur % (+ batch-count (count batches))))
        (apply-resolved-batches opts value {})))))
