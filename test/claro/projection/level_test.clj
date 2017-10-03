(ns claro.projection.level-test
  (:require [clojure.test.check :as tc]
            [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [clojure.test :refer :all]
            [claro.test :as test]
            [claro.data :as data]
            [claro.projection :as projection]
            [claro.projection.generators :as g]
            [claro.engine.fixtures :refer [make-engine]]))

(defspec t-level (test/times 50)
  (let [run!! (comp deref (make-engine))]
    (prop/for-all
      [n (gen/fmap (comp inc #(mod % 4)) gen/int)
       v (g/infinite-seq)]
      (let [value (projection/apply v (projection/levels n))]
        (= (->> (run!! value)
                (iterate :next)
                (take-while seq)
                (last)
                (:value))
           (+ (:n v) n -1))))))
