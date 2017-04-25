(ns claro.projection.transform-test
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

(defspec t-transform-with-output-template (test/times 100)
  (let [run! (make-engine)
        run!! (comp deref run! projection/apply)]
    (prop/for-all
      [initial-value (g/infinite-seq)
       replacement-value (g/infinite-seq-no-mutation)
       initial-template (g/valid-template)
       replacement-template (g/valid-template)]
      (let [observed (promise)
            transformation (projection/transform
                             (fn [v]
                               (deliver observed v)
                               replacement-value)
                             initial-template
                             replacement-template)]
        (and (is (= (run!! initial-value transformation)
                    (run!! replacement-value replacement-template)))
             (is (= (deref observed 0 ::none)
                    (run!! initial-value initial-template))))))))

(defspec t-transform-without-output-template (test/times 100)
  (let [run! (make-engine)
        run!! (comp deref run! projection/apply)]
    (prop/for-all
      [initial-value (g/infinite-seq)
       replacement-value (g/infinite-seq-no-mutation)
       initial-template (g/valid-template)
       replacement-template (g/valid-template)]
      (let [observed (promise)
            transformation (projection/transform
                             (fn [v]
                               (deliver observed v)
                               (projection/apply
                                 replacement-value
                                 replacement-template))
                             initial-template)]
        (and (is (= (run!! initial-value transformation)
                    (run!! replacement-value replacement-template)))
             (is (= (deref observed 0 ::none)
                    (run!! initial-value initial-template))))))))

(defspec t-transform-without-output-template-but-infinite-tree (test/times 25)
  (let [run! (make-engine {:max-cost 256})
        run!! (comp deref run! projection/apply)]
    (prop/for-all
      [initial-value (g/infinite-seq)
       replacement-value (g/infinite-seq-no-mutation)
       initial-template (g/valid-template)]
      (let [observed (promise)
            transformation (projection/transform
                             (fn [v]
                               (deliver observed v)
                               replacement-value)
                             initial-template)]
        (boolean
          (is
            (thrown-with-msg?
              IllegalStateException
              #"resolution has exceeded maximum cost"
              (run!! initial-value transformation))))))))

(defspec t-finite-transform-without-output-template-but-infinite-tree (test/times 25)
  (let [run! (make-engine {:max-cost 256})
        run!! (comp deref run! projection/apply)]
    (prop/for-all
      [initial-value (g/infinite-seq)
       replacement-value (g/infinite-seq-no-mutation)
       initial-template (g/valid-template)]
      (let [observed (promise)
            transformation (projection/transform-finite
                             (fn [v]
                               (deliver observed v)
                               replacement-value)
                             initial-template)]
        (= replacement-value (run!! initial-value transformation))))))
