(ns claro.engine.cost-test
  (:require [clojure.test.check :as tc]
            [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [clojure.test :refer :all]
            [claro.test :as test]
            [claro.data :as data]
            [manifold.deferred :as d]
            [claro.engine.fixtures :refer [make-engine]]))

;; ## Generators

(def ^:private gen-seed
  (gen/let [n     gen/pos-int
            delta gen/pos-int
            count gen/s-pos-int]
    (gen/return
      {:delta delta
       :seed  (take count (iterate #(+ delta %) n))})))

;; ## Tests

;; ### `Resolvable`

(defrecord PlainResolvable [n max-n]
  data/Resolvable
  (resolve! [_ _]
    (when (< n max-n)
      {:nested (PlainResolvable. (inc n) max-n)})))

(defspec t-plain-resolvable-cost (test/times 50)
  (prop/for-all
    [{:keys [seed delta]} gen-seed
     cost-per-resolvable  gen/s-pos-int]
    (let [max-cost   (* cost-per-resolvable (count seed))
          resolvable (map #(->PlainResolvable % (+ % delta)) seed)
          run!       (make-engine (atom []) {:max-cost max-cost})
          result     @(-> (run! resolvable) (d/catch identity))]
      (if (instance? Throwable result)
        (>= delta cost-per-resolvable)
        (if (= delta 0)
          (every? nil? result)
          (< delta cost-per-resolvable))))))

;; ### `BatchedResolvable`

(defrecord BatchedResolvable [n max-n]
  data/Resolvable
  data/BatchedResolvable
  (resolve-batch! [_ _ batch]
    (map
      (fn [{:keys [n max-n]}]
        (when (< n max-n)
          {:nested (BatchedResolvable. (inc n) max-n)}))
      batch)))

(defspec t-batched-resolvable-cost (test/times 50)
  (prop/for-all
    [{:keys [seed delta]} gen-seed
     max-cost             gen/s-pos-int]
    (let [resolvable (map #(->BatchedResolvable % (+ % delta)) seed)
          run!       (make-engine (atom []) {:max-cost max-cost})
          result     @(-> (run! resolvable) (d/catch identity))]
      (if (instance? Throwable result)
        (>= delta max-cost)
        (if (= delta 0)
          (every? nil? result)
          (< delta max-cost))))))

;; ### `PureResolvable`

(defrecord PlainPureResolvable [n max-n]
  data/PureResolvable
  data/Resolvable
  (resolve! [_ _]
    (when (< n max-n)
      {:nested (PlainPureResolvable. (inc n) max-n)})))

(defrecord BatchedPureResolvable [n max-n]
  data/Resolvable
  data/PureResolvable
  data/BatchedResolvable
  (resolve-batch! [_ _ batch]
    (map
      (fn [{:keys [n max-n]}]
        (when (< n max-n)
          {:nested (BatchedPureResolvable. (inc n) max-n)}))
      batch)))

(defspec t-plain-pure-resolvable-cost (test/times 50)
  (prop/for-all
    [{:keys [seed delta]} gen-seed
     cost-per-resolvable  gen/s-pos-int]
    (let [max-cost   (* cost-per-resolvable (count seed))
          resolvable (map #(->PlainPureResolvable % (+ % delta)) seed)
          run!       (make-engine (atom []) {:max-cost max-cost})
          result     @(-> (run! resolvable) (d/catch identity))]
      (if (= delta 0)
        (every? nil? result)
        (every? map? result)))))

(defspec t-batched-pure-resolvable-cost (test/times 50)
  (prop/for-all
    [{:keys [seed delta]} gen-seed
     cost-per-resolvable  gen/s-pos-int]
    (let [max-cost   (* cost-per-resolvable (count seed))
          resolvable (map #(->BatchedPureResolvable % (+ % delta)) seed)
          run!       (make-engine (atom []) {:max-cost max-cost})
          result     @(-> (run! resolvable) (d/catch identity))]
      (if (= delta 0)
        (every? nil? result)
        (every? map? result)))))

;; ## Custom Resolvable Cost

(defrecord CustomCostResolvable [n max-n]
  data/Resolvable
  (resolve! [_ _]
    (when (< n max-n)
      {:nested (CustomCostResolvable. (inc n) max-n)}))

  data/Cost
  (cost [_ batch]
    (* 2 (count batch))))

(defspec t-custom-resolvable-cost (test/times 50)
  (prop/for-all
    [{:keys [seed delta]} gen-seed
     cost-per-resolvable  gen/s-pos-int]
    (let [max-cost   (* cost-per-resolvable (count seed) 2)
          resolvable (map #(->CustomCostResolvable % (+ % delta)) seed)
          run!       (make-engine (atom []) {:max-cost max-cost})
          result     @(-> (run! resolvable) (d/catch identity))]
      (if (instance? Throwable result)
        (>= delta cost-per-resolvable)
        (if (= delta 0)
          (every? nil? result)
          (< delta cost-per-resolvable))))))
