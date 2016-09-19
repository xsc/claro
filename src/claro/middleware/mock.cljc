(ns claro.middleware.mock
  "Generic I/O mocking middlewares."
  (:require [claro.engine.core :as engine]
            [claro.data.protocols :as p]
            [claro.runtime.impl :as impl]))

;; ## Mock Wrapper

(deftype Mock [mock-fn original transform?]
  p/Resolvable
  (resolve! [_ env]
    (mock-fn original env))

  p/Transform
  (transform [_ result]
    (if transform?
      (p/transform original result)
      result)))

(alter-meta! #'->Mock assoc :private true)

(defn- ->mock-with-transform
  [mock-fn value]
  (->Mock mock-fn value true))

(defn- ->mock-without-transform
  [mock-fn value]
  (->Mock mock-fn value false))

;; ## Logic

(defn- wrap-mocks
  [->mock batch]
  (map ->mock batch))

(defn- unwrap-mocks
  [engine deferred]
  (impl/chain1
    (engine/impl engine)
    deferred
    #(->> (for [[^Mock mock resolved] %]
            [(.-original mock) resolved])
          (into {}))))

(defn- mock-resolver
  [->mock engine resolver env batch]
  (->> batch
       (wrap-mocks ->mock)
       (resolver env)
       (unwrap-mocks engine)))

(defn- make-lookup-fn
  [class mock-fn more]
  {:pre [(even? (count more))]}
  (comp
    (->> (partition 2 more)
         (into {class mock-fn}))
    clojure.core/class
    first))

(defn- make-mock-wrapper
  [engine ->mock lookup-fn]
  (fn [resolver]
    (fn [env batch]
      (if-let [mock-fn (lookup-fn batch)]
        (mock-resolver
          (partial ->mock mock-fn)
          engine
          resolver
          env
          batch)
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
       (make-mock-wrapper engine ->mock-with-transform)
       (engine/wrap engine)))

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
       (make-mock-wrapper engine ->mock-without-transform)
       (engine/wrap engine)))
