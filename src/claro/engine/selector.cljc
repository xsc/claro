(ns claro.engine.selector)

;; ## Protocol

(defprotocol Selector
  (instantiate [selector]
    "Generate a selector instance, i.e. a function taking a map of
     classes/resolvables and returning a seq of classes to resolve during
     the current iteration step."))

;; ## Selectors

(def default-selector
  "Always selects all available classes."
  (reify Selector
    (instantiate [_]
      keys)))

(defn parallel-selector
  "Select at most `n` classes to resolve during each iteration."
  [n]
  {:pre [(pos? n)]}
  (reify Selector
    (instantiate [_]
      (fn [class->resolvables]
        (take n (keys class->resolvables))))))

(defn scoring-selector
  "Use `(score-fn class resolvables)` to generate a score for each
   class, selecting the one with the highest score.

   For example, to always select the class with the most resolvables:

   ```clojure
   (scoring-selector
     (fn [class resolvables]
       (count resolvables)))
   ```

   Or, to assign a score based on class, e.g always resolve `FriendsOf` before
   `Person` if applicable:

   ```clojure
   (scoring-selector
     (fn [class resolvables]
       (get {Person 2, FriendsOf 1} class 0)))
   ```
   "
  [score-fn]
  (reify Selector
    (instantiate [_]
      (fn [class->resolvables]
        (->> class->resolvables
             (apply max-key #(score-fn (key %) (val %)))
             (key)
             (vector))))))

(defn exact-selector
  "Select the given classes in the order they are given, e.g.:

   ```clojure
   (exact-selector
     [[Person]
      [FriendsOf FatherOf]
      [Person]])
   ```

   This can be used on a per-resolution basis, ideally providing the optimal
   resolution order:

   ```clojure
   (engine/run!
     {:selector (exact-selector ...)}
     (->Person 1))
   ```
   "
  [class-batches & [fallback-selector]]
  (let [fallback-selector (or fallback-selector default-selector)
        class-batches (map set class-batches)]
    (reify Selector
      (instantiate [_]
        (let [remaining (volatile! class-batches)
              fallback  (instantiate fallback-selector)]
          (fn [class->resolvables]
            (let [next-batch (some-> (first @remaining)
                                     (filter (keys class->resolvables)))]
              (vswap! remaining next)
              (if (seq next-batch)
                next-batch
                (fallback class->resolvables)))))))))
