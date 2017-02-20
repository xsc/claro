(ns claro.middleware.transform-test
  (:require [clojure.test :refer :all]
            [claro.data :as data]
            [claro.engine :as engine]
            [claro.middleware.transform :refer :all]))

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

;; ## Transform Engines

(defn- run-transform
  [class input]
  (let [run (-> (engine/engine)
                (wrap-transform
                  #(instance? class %)
                  #(assoc %2 :__timestamp (System/currentTimeMillis))))]
    @(run input)))

(defn- run-transform-by-class
  [class input]
  (let [run (-> (engine/engine)
                (wrap-transform-by-class
                  [class]
                  #(assoc %2 :__timestamp (System/currentTimeMillis))))]
    @(run input)))

;; ## Tests

(deftest t-wrap-transform
  (let [input {:people         (map ->Person (range 5))
               :batched-people (map ->BatchedPerson (range 6 10))}]
    (testing "transforming a `Resolvable`."
      (let [{:keys [people batched-people]}
            (run-transform Person input)]
        (is (every? :__timestamp people))
        (is (not-any? :__timestamp batched-people))))
    (testing "transforming a `BatchedResolvable`."
      (let [{:keys [people batched-people]}
            (run-transform BatchedPerson input)]
        (is (not-any? :__timestamp people))
        (is (every? :__timestamp batched-people))))))

(deftest t-wrap-transform-by-class
  (let [input {:people         (map ->Person (range 5))
               :batched-people (map ->BatchedPerson (range 6 10))}]
    (testing "transforming a `Resolvable`."
      (let [{:keys [people batched-people]}
            (run-transform-by-class Person input)]
        (is (every? :__timestamp people))
        (is (not-any? :__timestamp batched-people))))
    (testing "transforming a `BatchedResolvable`."
      (let [{:keys [people batched-people]}
            (run-transform-by-class BatchedPerson input)]
        (is (not-any? :__timestamp people))
        (is (every? :__timestamp batched-people))))))
