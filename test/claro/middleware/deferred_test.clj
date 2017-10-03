(ns claro.middleware.deferred-test
  (:require [clojure.test :refer :all]
            [claro.data :as data]
            [claro.engine :as engine]
            [claro.middleware.deferred :refer :all]
            [manifold.deferred :as d]))

(defrecord Person [id]
  data/Resolvable
  (resolve! [_ env]
    ::ok))

(deftest t-wrap-deferred
  (let [run (-> (engine/engine)
                (wrap-deferred
                  #(instance? Person (first %))
                  (fn [env deferred]
                    (d/chain
                      deferred
                      #(->> (for [[resolvable result] %]
                              [resolvable ::uniform])
                            (into {}))))))]
    (is (= ::ok @(engine/run! (->Person 1))))
    (is (= ::uniform @(run (->Person 1))))))
