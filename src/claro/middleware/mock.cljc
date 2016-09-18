(ns claro.middleware.mock
  (:require [claro.engine.core :as engine]
            [claro.data.protocols :as p]
            [claro.runtime.impl :as impl]))

;; ## Mock Wrapper

(deftype Mock [mock-fn original]
  p/Resolvable
  (resolve! [_ env]
    (mock-fn original env))

  p/Transform
  (transform [_ result]
    (p/transform original result)))

(alter-meta! #'->Mock assoc :private true)

;; ## Logic

(defn- wrap-mocks
  [mock-fn batch]
  (map #(->Mock mock-fn %) batch))

(defn- unwrap-mocks
  [engine deferred]
  (impl/chain1
    (engine/impl engine)
    deferred
    #(->> (for [[^Mock mock resolved] %]
            [(.-original mock) resolved])
          (into {}))))

(defn- mocked-resolver
  [engine resolver mock-fn env batch]
  (->> batch
       (wrap-mocks mock-fn)
       (resolver env)
       (unwrap-mocks engine)))

;; ## Middleware

(defn wrap-mock
  "Middleware that will prevent calling of `resolve!` or `resolve-batch!` for
   the given class, but instead use the given `mock-fn` to compute a result
   for each `Resolvable`. `mock-fn` takes the resolvable in question, as well
   as the environment as parameters.

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

   > __Note:__ Multiple class/mock-fn pairs can be given.

   Transformations declared by implementing the [[Transform]] protocol will
   still be performed."
  [engine class mock-fn & more]
  {:pre [(even? (count more))]}
  (let [mock-fn-for (comp
                      (->> (partition 2 more)
                           (into {class mock-fn}))
                      clojure.core/class)]
    (->> (fn [resolver]
           (fn [env [resolvable :as batch]]
             (if-let [mock-fn (mock-fn-for resolvable)]
               (mocked-resolver engine resolver mock-fn env batch)
               (resolver env batch))))
         (engine/wrap engine))))
