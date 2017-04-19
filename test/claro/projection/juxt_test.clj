(ns claro.projection.juxt-test
  (:require [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [clojure.test :refer :all]
            [claro.test :as test]
            [claro.projection.generators :as g]
            [claro.engine.fixtures :refer [make-engine]]
            [claro.data.ops.then :refer [then]]
            [claro.projection :as projection]))

(defn sum-nested-values
  [initial]
  (->> (projection/juxt
         (projection/extract :value)
         (projection/extract-in [:next :value])
         (projection/extract-in [:next :next :value]))
       (projection/transform #(reduce + initial %))))

(defspec t-juxt-projection (test/times 100)
  (let [run! (make-engine)]
    (prop/for-all
      [value   (g/infinite-seq)
       initial gen/int]
      (let [template (sum-nested-values initial)
            result   @(run! (projection/apply value template))
            expected (->> (iterate inc (:n value))
                          (take 3)
                          (reduce + initial))]
        (= expected result)))))
