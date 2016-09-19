(ns claro.middleware.mock-test
  (:require [clojure.test :refer :all]
            [claro.data :as data]
            [claro.engine :as engine]
            [claro.middleware.mock :refer :all]))

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

;; ## Mock Engines

(defn- run-mock
  [class input]
  (let [run (-> (engine/engine)
                (wrap-mock
                  class
                  (constantly {:name "Me", :friend-ids []})))]
    @(run input)))

(defn- run-mock-result
  [class input]
  (let [run (-> (engine/engine)
                (wrap-mock-result
                  class
                  (constantly {:name "Me", :friend-ids []})))]
    @(run input)))

;; ## Tests

(deftest t-wrap-mock
  (testing "mocking a `Resolvable`."
    (let [input (map ->Person (range 5))]
      (testing "actual resolution results."
        (is (= (map
                 (fn [{:keys [id]}]
                   (assoc (fetch-person id) :id id))
                 input)
               (engine/run!! input))))
      (testing "mocked results."
        (is (= (map
                 (fn [{:keys [id]}]
                   {:id id
                    :name "Me"
                    :friend-ids []})
                 input)
               (run-mock Person input)))))

    (testing "mocking a `BatchedResolvable`."
      (let [input (map ->BatchedPerson (range 5))]
        (testing "actual resolution results."
          (is (= (map
                   (fn [{:keys [id]}]
                     (assoc (fetch-person id) :id id))
                   input)
                 (engine/run!! input))))
        (testing "mocked results."
          (is (= (map
                   (fn [{:keys [id]}]
                     {:id id
                      :name "Me"
                      :friend-ids []})
                   input)
                 (run-mock BatchedPerson input))))))))

(deftest t-wrap-mock-result
  (testing "mocking a `Resolvable`."
    (let [input (map ->Person (range 5))]
      (is (= (map
               (fn [{:keys [id]}]
                 {:name "Me"
                  :friend-ids []})
               input)
             (run-mock-result Person input))))

    (testing "mocking a `BatchedResolvable`."
      (let [input (map ->BatchedPerson (range 5))]
        (is (= (map
                 (fn [{:keys [id]}]
                   {:name "Me"
                    :friend-ids []})
                 input)
               (run-mock-result BatchedPerson input)))))))
