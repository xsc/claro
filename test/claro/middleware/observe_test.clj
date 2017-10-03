(ns claro.middleware.observe-test
  (:require [clojure.test :refer :all]
            [claro.data :as data]
            [claro.engine :as engine]
            [claro.middleware.observe :refer :all]))

;; ## Resolvables

(defn- fetch-person
  [id]
  {:name       (str "Person #" id)
   :friend-ids (range (inc id) (+ id 10) 3)})

(defrecord Person [id]
  data/Resolvable
  (resolve! [_ env]
    (fetch-person id))
  data/Transform
  (transform [_ data]
    (assoc data :id id)))

(defrecord BatchedPerson [id]
  data/Resolvable
  data/BatchedResolvable
  (resolve-batch! [_ env people]
    (map (comp fetch-person :id) people))
  data/Transform
  (transform [_ data]
    (assoc data :id id)))

;; ## Observation Helper

(defn- run-observe-all
  [input]
  (let [observed (atom #{})
        run (-> (engine/engine)
                (wrap-observe #(swap! observed conj (vec %&))))]
    @(run input)
    @observed))

(defn- run-observe
  [predicate input]
  (let [observed (atom #{})
        run (-> (engine/engine)
                (wrap-observe
                  predicate
                  #(swap! observed conj (vec %&))))]
    @(run input)
    @observed))

(defn- run-observe-by-class
  [class input]
  (let [observed (atom #{})
        run (-> (engine/engine)
                (wrap-observe-by-class
                  class
                  #(swap! observed conj (vec %&))))]
    @(run input)
    @observed))

(defn- run-observe-batches
  [predicate input]
  (let [observed (atom #{})
        run (-> (engine/engine)
                (wrap-observe-batches
                  predicate
                  #(swap! observed conj %)))]
    @(run input)
    @observed))

(defn- run-observe-batches-by-class
  [class input]
  (let [observed (atom #{})
        run (-> (engine/engine)
                (wrap-observe-batches-by-class
                  class
                  #(swap! observed conj %)))]
    @(run input)
    @observed))

;; ## Fixtures

(defn transform-results
  [sq]
  (map
    (fn [[resolvable result]]
      [resolvable (data/transform resolvable result)])
    sq))

(def input
  {:people         (map ->Person (range 5))
   :batched-people (map ->BatchedPerson (range 6 10))})

(def people-tuples
  (->> (:people input)
       (map (juxt identity #(data/resolve! % {})))
       (transform-results)))

(def batched-people-tuples
  (let [ps (:batched-people input)]
    (->> (data/resolve-batch! (first ps) {} ps)
         (map vector ps)
         (transform-results))))

;; ## Tests

(deftest t-wrap-observe
  (testing "observing all resolvables."
    (let [observed (run-observe-all input)]
      (is (= (set (concat people-tuples
                          batched-people-tuples))
             observed))))
  (testing "observing using predicate."
    (let [observed (run-observe #(instance? Person %) input)]
      (is (= (set people-tuples) observed)))
    (let [observed (run-observe #(instance? BatchedPerson %) input)]
      (is (= (set batched-people-tuples) observed)))))

(deftest t-wrap-observe-by-class
  (testing "observing by class."
    (let [observed (run-observe-by-class [BatchedPerson Person] input)]
      (is (= (set (concat batched-people-tuples people-tuples))
             observed)))
    (let [observed (run-observe-by-class [Person] input)]
      (is (= (set people-tuples) observed)))
    (let [observed (run-observe-by-class [BatchedPerson] input)]
      (is (= (set batched-people-tuples) observed)))))

(deftest t-wrap-observe-batches
  (testing "observing batches using predicate."
    (let [observed (run-observe-batches #(instance? Person (first %)) input)]
      (is (= (hash-set (into {} people-tuples)) observed)))
    (let [observed (run-observe-batches
                     #(instance? BatchedPerson (first %)) input)]
      (is (= (hash-set (into {} batched-people-tuples)) observed)))))

(deftest t-wrap-observe-batches-by-class
  (testing "observing batches using predicate."
    (let [observed (run-observe-batches-by-class [BatchedPerson Person] input)]
      (is (= (hash-set
               (into {} people-tuples)
               (into {} batched-people-tuples))
             observed)))
    (let [observed (run-observe-batches-by-class [Person] input)]
      (is (= (hash-set (into {} people-tuples)) observed)))
    (let [observed (run-observe-batches-by-class [BatchedPerson] input)]
      (is (= (hash-set (into {} batched-people-tuples)) observed)))))
