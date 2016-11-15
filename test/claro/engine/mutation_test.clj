(ns claro.engine.mutation-test
  (:require [clojure.test :refer :all]
            [claro.data :as data]
            [claro.engine.fixtures :refer [make-engine]]))

;; ## Records

(defrecord CounterValue []
  data/Resolvable
  (resolve! [_ {:keys [counter]}]
    @counter))

(defrecord Increment []
  data/Mutation
  data/Resolvable
  (resolve! [_ {:keys [counter]}]
    (swap! counter inc)
    (->CounterValue)))

(defrecord IncrementTwo []
  data/Mutation
  data/Resolvable
  (resolve! [_ {:keys [counter]}]
    (swap! counter + 2)
    (->CounterValue)))

(defrecord IllegalIncrement []
  data/Resolvable
  (resolve! [_ _]
    (->Increment)))

;; ## Tests

(deftest t-mutations
  (let [counter (atom 0)
        run! (make-engine (atom []) {:env {:counter counter}})]
    (testing "successful mutation resolution."
      (are [value expected] (= expected @(run! value))
           (->Increment)                 1
           {:result (->Increment)}       {:result 2}
           [(->Increment) (->Increment)] [3 3]))
    (testing "mutation resolution constraint violations."
      (are [value re] (thrown-with-msg?
                        IllegalStateException
                        re
                        @(run! value))
           (->IllegalIncrement)
           #"can only resolve mutations on the top-level"


           [(->Increment) (->IncrementTwo)]
           #"only one mutation can be resolved per engine run"))))
