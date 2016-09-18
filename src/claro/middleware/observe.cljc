(ns claro.middleware.observe
  "Generic middlewares to observe resolution results."
  (:require [claro.engine.core :as engine]
            [claro.runtime.impl :as impl]))

;; ## Observe Logic

(defn- observe!
  [predicate observer-fn result]
  (doseq [[resolvable resolved] result
          :when (predicate resolvable)]
    (observer-fn resolvable resolved))
  result)

;; ## Middlewares

(defn wrap-observe
  "Middleware that will pass any `Resolvable` that matches `predicate` – as well
   as the resolved result – to `observer-fn`.

   For example, to check the result of a specific `Person` record:

   ```clojure
   (def run-engine
     (-> (engine/engine)
         (wrap-observe
           #(and (instance? Person %) (= (:id %) 1))
           prn)))
   ```

   If no `predicate` is given, all `Resolvable` values will be observed."
  ([engine observer-fn]
   (wrap-observe engine (constantly true) observer-fn))
  ([engine predicate observer-fn]
   {:pre [(ifn? predicate) (ifn? observer-fn)]}
   (->> (fn [resolver]
          (fn [env batch]
            (impl/chain1
              (engine/impl engine)
              (resolver env batch)
              #(observe! predicate observer-fn %))))
        (engine/wrap engine))))

(defn wrap-observe-by-class
  "Middleware that will pass any `Resolvable` of one of the given
   `classes-to-observe` – as well as the resolved result – to `observer-fn`.

   For example, to check the result of a specific `Person` record:

   ```clojure
   (def run-engine
     (-> (engine/engine)
         (wrap-observe-classes [Person] prn)))
   ```
   "
  [engine classes-to-observe observer-fn]
  {:pre [(seq classes-to-observe)]}
  (let [predicate (fn [resolvable]
                    (some
                      #(instance? % resolvable)
                      classes-to-observe))]
    (wrap-observe engine predicate observer-fn)))

(defn wrap-observe-batches
  "Middleware that will pass the result of any batch matching `predicate` to
   `observer-fn`.

   For example, to increase a total resolvable counter for `Person` records:

   ```clojure
   (def run-engine
     (-> (engine/engine)
         (wrap-observe-batches
           #(instance? Person (first %))
           (fn [result]
             (swap! counter + (count result))))))
   ```

   If no `predicate` is given, every batch will be observed."
  ([engine observer-fn]
   (wrap-observe-batches engine (constantly true) observer-fn))
  ([engine predicate observer-fn]
   (->> (fn [resolver]
          (fn [env batch]
            (impl/chain1
              (engine/impl engine)
              (resolver env batch)
              (fn [result]
                (when (predicate result)
                  (observer-fn result))
                result))))
        (engine/wrap engine))))

(defn wrap-observe-batches-by-class
  "Middleware that will pass the result of any batch of `classes-to-observe`
   to `observer-fn`.

   For example, to increase a total resolvable counter for `Person` records:

   ```clojure
   (def run-engine
     (-> (engine/engine)
         (wrap-observe-batches-by-class
           [Person]
           (fn [result]
             (swap! counter + (count result))))))
   ```
   "
  [engine classes-to-observe observer-fn]
  {:pre [(seq classes-to-observe)]}
  (let [predicate (fn [[resolvable]]
                    (some
                      #(instance? % resolvable)
                      classes-to-observe))]
    (wrap-observe-batches engine predicate observer-fn)))

(defn wrap-observe-duration
  "Identical to [[wrap-observe-batches]], except that `observer-fn` is passed an
   additional argument representing the resolution duration in nanoseconds."
  ([engine observer-fn]
   (wrap-observe-batches-duration engine (constantly true) observer-fn))
  ([engine predicate observer-fn]
   (->> (fn [resolver]
          (fn [env batch]
            (let [start-time (System/nanoTime)]
              (impl/chain1
                (engine/impl engine)
                (resolver env batch)
                (fn [result]
                  (let [delta (- (System/nanoTime) start-time)]
                    (when (predicate result)
                      (observer-fn result delta)))
                  result)))))
        (engine/wrap engine))))
