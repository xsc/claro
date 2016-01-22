(ns claro.engine.caching-test
  (:require [clojure.test :refer :all]
            [claro.data :as data]
            [claro.engine.fixtures :refer [make-engine]]))

(deftest t-caching
  (let [counter (atom 0)
        counter-resolvable (reify data/Resolvable
                             (resolve! [_ _]
                               (swap! counter inc)))
        nested-resolvable  (reify data/Resolvable
                             (resolve! [_ _]
                               counter-resolvable))
        run! (make-engine)]
    (is (= 1 @(run! counter-resolvable)))
    (is (= {:a 2, :b 2} @(run! {:a counter-resolvable, :b counter-resolvable})))
    (is (= {:a 3, :b 3} @(run! {:a counter-resolvable, :b nested-resolvable})))))
