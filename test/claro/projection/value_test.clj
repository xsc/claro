(ns claro.projection.value-test
  (:require [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [clojure.test :refer :all]
            [claro.test :as test]
            [claro.projection.generators :as g]
            [claro.engine.fixtures :refer [make-engine]]
            [claro.projection :as projection]))

(defspec t-value-injection (test/times 200)
  (let [run! (make-engine)]
    (prop/for-all
      [template        (g/valid-template)
       value           (g/infinite-seq)
       value-to-inject gen/simple-type-printable]
      (let [template-with-injection
            (assoc template
                   :extra-value
                   (projection/value value-to-inject))
            projected-value
            (projection/apply value template)
            projected-value-with-injection
            (projection/apply value template-with-injection)]
        (= @(run! projected-value-with-injection)
           (-> @(run! projected-value)
               (assoc :extra-value value-to-inject)))))))

(defspec t-value-override (test/times 200)
  (let [run! (make-engine)]
    (prop/for-all
      [value           (g/infinite-seq)
       value-to-inject gen/simple-type-printable]
      (let [template {:next (projection/value value-to-inject)}
            projected-value (projection/apply value template)]
        (= {:next value-to-inject}
           @(run! projected-value))))))
