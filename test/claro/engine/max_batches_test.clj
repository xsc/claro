(ns claro.engine.max-batches-test
  (:require [clojure.test.check :as tc]
            [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [clojure.test :refer :all]
            [claro.test :as test]
            [claro.data :as data]
            [claro.engine.fixtures :refer [make-engine]]))

(defrecord Nested [n max-n]
  data/Resolvable
  (resolve! [_ _]
    (when (< n max-n)
      {:nested (Nested. (inc n) max-n)})))

(defspec t-max-batches (test/times 100)
  (prop/for-all
    [max-n       gen/pos-int
     max-batches gen/s-pos-int]
    (let [run! (make-engine (atom []) {:max-batches max-batches})
          result (run! (Nested. 0 max-n))]
      (cond (= max-n 0)           (is (nil? @result))
            (< max-n max-batches) (is (map? @result))
            :else (boolean
                    (is (thrown-with-msg?
                          IllegalStateException
                          #"resolution has exceeded maximum batch count/depth"
                          @result)))))))
