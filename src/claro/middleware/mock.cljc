(ns claro.middleware.mock
  "Generic I/O mocking middlewares."
  (:require [claro.engine.core :as engine]
            [claro.data.protocols :as p]
            [claro.runtime.impl :as impl]))

;; ## Logic

(defn- make-lookup-fn
  [class mock-fn more]
  {:pre [(even? (count more))]}
  (comp
    (->> (partition 2 more)
         (into {class mock-fn}))
    clojure.core/class
    first))

(defn- make-mock-wrapper
  [engine lookup-fn]
  (fn [resolver]
    (fn [env batch]
      (if-let [mock-fn (lookup-fn batch)]
        (impl/value
          (engine/impl engine)
          (->> (for [resolvable batch]
                 [resolvable (mock-fn resolvable env)])
               (into {})))
        (resolver env batch)))))

;; ## Middlewares

(defn wrap-mock
  "Middleware that will prevent calling of `resolve!` or `resolve-batch!` for
   the given class, but instead use the given `mock-fn` to compute a result
   for each `Resolvable`. `mock-fn` takes the resolvable in question, as well
   as the environment as parameters.

   Transformations declared by implementing the [[Transform]] protocol will
   still be performed.

   For example, to mock resolution of `Person` records:

   ```clojure
   (def run-engine
     (-> (engine/engine)
         (wrap-mock
           Person
           (fn [{:keys [id]} env]
             {:id id
              :name (str \"Person #\" id)}))))
   ```

   > __Note:__ Multiple class/mock-fn pairs can be given."
  [engine class mock-fn & more]
  (->> (make-lookup-fn class mock-fn more)
       (make-mock-wrapper engine)
       (engine/wrap-pre-transform engine)))

(defn wrap-mock-result
  "Middleware that will prevent calling of `resolve!` or `resolve-batch!` for
   the given class, but instead use the given `mock-fn` to compute a result
   for each `Resolvable`. `mock-fn` takes the resolvable in question, as well
   as the environment as parameters.

   Transformations declared by implementing the [[Transform]] protocol will
   be ignored, so this really has to return the eventual result.

   For example, to mock resolution of `Person` records:

   ```clojure
   (def run-engine
     (-> (engine/engine)
         (wrap-mock-result
           Person
           (fn [{:keys [id]} env]
             {:id      id
              :name    (str \"Person #\" id)
              :friends (map ->Person (range id))}))))
   ```

   > __Note:__ Multiple class/mock-fn pairs can be given."
  [engine class mock-fn & more]
  (->> (make-lookup-fn class mock-fn more)
       (make-mock-wrapper engine)
       (engine/wrap-transform engine)))
