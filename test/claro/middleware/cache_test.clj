(ns claro.middleware.cache-test
  (:require [clojure.test.check :as tc]
            [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [clojure.test :refer :all]
            [clojure.set :as set]
            [claro.test :as test]
            [claro.data :as data]
            [claro.engine :as engine]
            [claro.middleware.cache :refer :all]))

;; ## Resolvable

(defonce counter (atom 0))

(defrecord Counter [id]
  data/Resolvable
  (resolve! [_ _]
    (swap! counter inc)))

(defrecord CounterPure [id]
  data/PureResolvable
  data/Resolvable
  (resolve! [_ _]
    (swap! counter inc)))

;; ## Cache

(defrecord AtomCache [cache]
  ResolvableCache
  (cache-get
    [_ _ resolvables]
    (swap! cache vary-meta update :gets (fnil + 0) (count resolvables))
    (select-keys @cache resolvables))
  (cache-put
    [_ _ result]
    (swap! cache vary-meta update :puts (fnil + 0) (count result))
    (swap! cache merge result))

  clojure.lang.IDeref
  (deref [_]
    @cache))

(defn atom-cache
  []
  (->AtomCache (atom {})))

;; ## Helper

(defn- run-with-cache
  [cache value]
  (let [run (-> (engine/engine {:check-cost? false})
                (wrap-cache cache))]
    @(run value)))

(defn- submap?
  [v1 v2]
  (set/subset? (set v1) (set v2)))

;; ## Tests

(defspec t-wrap-cache (test/times 100)
  (let [cache (atom-cache)]
    (prop/for-all
      [values (->> (gen/fmap ->Counter gen/pos-int)
                   (gen/vector)
                   (gen/not-empty))]
      (let [cache-before @cache
            result       (run-with-cache cache values)
            cache-after  @cache]
        (and (not (empty? cache-after))
             (submap? cache-before cache-after))))))

(defspec t-wrap-cache-ignores-pure-resolvables (test/times 100)
  (let [cache (atom-cache)]
    (prop/for-all
      [values (gen/vector (gen/fmap ->CounterPure gen/pos-int))]
      (let [cache-before @cache
            result       (run-with-cache cache values)
            cache-after  @cache]
        (and (empty? cache-before)
             (empty? cache-after))))))
