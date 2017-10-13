(ns claro.middleware.intercept
  "A middleware to allow partial resolution of batches using a custom resolver."
  (:require [claro.data.protocols :as p]
            [claro.engine :as engine]
            [claro.runtime.impl :as impl]))

;; ## Logic

(defn- resolve-remaining
  [impl resolver env batch interception-result]
  (let [intercepted (set (keys interception-result))]
    (if (empty? intercepted)
      (resolver env batch)
      (if-let [remaining (seq (remove intercepted batch))]
        (impl/chain1
          impl
          (resolver env remaining)
          #(merge interception-result %))
        interception-result))))

;; ## Middleware

(defn ^{:added "0.2.20"} wrap-intercept
  "Wrap the given engine to allow resolution of partial batches using different
   means (e.g. a cache lookup).

   ```clojure
   (defonce engine
     (-> (engine/engine)
         (wrap-intercept
           (fn [env batch]
             (lookup-in-cache! env batch)))))
   ```

   `intercept-fn` has to return a deferred value with a map associating `batch`
   elements with their result. Everything that was not resolved will be passed
   to the original resolution logic.

   Note that `PureResolvable` batches will never be passed to the interceptor."
  [engine intercept-fn]
  (let [impl (engine/impl engine)]
    (->> (fn [resolver]
           (fn [env [resolvable :as batch]]
             (if-not (p/pure-resolvable? resolvable)
               (impl/chain1
                 impl
                 (intercept-fn env batch)
                 #(resolve-remaining impl resolver env batch %))
               (resolver env batch))))
         (engine/wrap-pre-transform engine))))
