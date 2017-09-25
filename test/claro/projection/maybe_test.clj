(ns claro.projection.maybe-test
  (:require [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [clojure.test :refer :all]
            [claro.test :as test]
            [claro.projection.generators :as g]
            [claro.engine.fixtures :refer [make-engine]]
            [claro.data.ops :as ops]
            [claro.projection :as projection]))

(defspec t-maybe (test/times 200)
  (let [run! (make-engine)]
    (prop/for-all
      [template (g/valid-template)
       value    (gen/one-of
                  [(g/infinite-seq)
                   (gen/return (g/->Identity nil))])]
      (let [maybe-template  (projection/maybe template)
            should-be-nil?  (contains? value :value)
            projected-value (projection/apply value maybe-template)
            result          @(run! projected-value)]
        (if should-be-nil?
          (nil? result)
          (= result @(run! (projection/apply value template))))))))

(defspec t-default (test/times 200)
  (let [run! (make-engine)]
    (prop/for-all
      [template (g/valid-template)
       value    (gen/one-of
                  [(g/infinite-seq)
                   (gen/return (g/->Identity nil))])
       default-value (g/infinite-seq-no-mutation)]
      (let [default-template (projection/default template default-value)
            should-be-nil?   (contains? value :value)
            projected-value  (projection/apply value default-template)
            result           @(run! projected-value)]
        (if should-be-nil?
          (= result @(run! (projection/apply default-value template)))
          (= result @(run! (projection/apply value template))))))))

(deftest t-maybe-on-missing-key
  (let [run! (make-engine)
        value {:a 0}
        template {:b (projection/maybe projection/leaf)}]
    (is (= {:b nil}
           @(run! (projection/apply value template))))))

(deftest t-default-on-missing-key
  (let [run! (make-engine)
        value {:a 0}
        template {:b (projection/default projection/leaf 4)}]
    (is (= {:b 4}
           @(run! (projection/apply value template))))))
