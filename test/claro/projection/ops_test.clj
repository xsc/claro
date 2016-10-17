(ns claro.projection.ops-test
  (:require [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [clojure.test :refer :all]
            [claro.test :as test]
            [claro.data.ops :as ops]
            [claro.projection.generators :as g]
            [claro.engine.fixtures :refer [make-engine]]
            [claro.projection :as projection]))

(defspec t-projection-on-ops-in-seq (test/times 100)
  (let [run! (make-engine)]
    (prop/for-all
      [template (g/valid-template)
       value    (g/infinite-seq)]
      (let [projected-value (projection/apply
                              [(ops/then value identity)]
                              [template])
            result @(run! projected-value)]
        (g/compare-to-template (first result) template (:n value))))))
