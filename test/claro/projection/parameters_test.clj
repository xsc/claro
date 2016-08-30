(ns claro.projection.parameters-test
  (:require [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [clojure.test :refer :all]
            [claro.test :as test]
            [claro.projection.generators :as g]
            [claro.engine.fixtures :refer [make-engine]]
            [claro.data :as data]
            [claro.projection :as projection]))

;; ## Record

(defrecord ParameterizableSeq []
  data/Resolvable
  (resolve! [_ _]
    {:infinite-seq (g/->InfiniteSeq nil)}))

(defrecord ParameterizableSeqs []
  data/Resolvable
  (resolve! [_ _]
    {:infinite-seqs
     [(g/->InfiniteSeq nil) (g/->InfiniteSeq nil)]}))

;; ## Tests

(defspec t-parameterizable-seq-needs-value (test/times 5)
  ;; verifies that without parameters, resolution fails
  (let [run! (make-engine)]
    (prop/for-all
      [template (g/valid-template)]
      (boolean
        (is
          (thrown?
            NullPointerException
            @(-> (->ParameterizableSeq)
                 (projection/apply
                   {:infinite-seq  template})
                 (run!))))))))

(defspec t-parameters (test/times 50)
  (let [run! (make-engine)]
    (prop/for-all
      [template (g/valid-template)
       n        gen/int]
      (let [{:keys [infinite-seq]}
            @(-> (->ParameterizableSeq)
                 (projection/apply
                   {:infinite-seq
                    (projection/parameters {:n n} template)})
                 (run!))]
        (is (g/compare-to-template infinite-seq template n))))))

(defspec t-parameters-without-resolvable (test/times 10)
  (let [run! (make-engine)]
    (prop/for-all
      [template (g/valid-template)
       value    gen/any
       n        gen/nat]
      (boolean
        (is
          (thrown-with-msg?
            IllegalArgumentException
            #"requires a resolvable"
            @(-> value
                 (projection/apply
                   (projection/parameters {:n n} template))
                 (run!))))))))

(defspec t-missing-parameter-field (test/times 10)
  (let [run! (make-engine)]
    (prop/for-all
      [template (g/valid-template)
       n        gen/nat]
      (boolean
        (is
          (thrown-with-msg?
            IllegalArgumentException
            #"requires key ':m' to exist"
            @(-> (->ParameterizableSeq)
                 (projection/apply
                   {:infinite-seq (projection/parameters {:m n} template)})
                 (run!))))))))

(defspec t-non-nil-parameter-field (test/times 10)
  (let [run! (make-engine)]
    (prop/for-all
      [template (g/valid-template)
       value    (g/infinite-seq)
       n        gen/nat]
      (boolean
        (is
          (thrown-with-msg?
            IllegalArgumentException
            #"cannot override non-nil value"
            @(-> value
                 (projection/apply
                   (projection/parameters {:n n} template))
                 (run!))))))))

(defspec t-parameters-within-seq (test/times 100)
  (let [run! (make-engine)]
    (prop/for-all
      [template (g/valid-template)
       n        gen/int]
      (let [{:keys [infinite-seqs]}
            @(-> (->ParameterizableSeqs)
                 (projection/apply
                   {:infinite-seqs
                    [(projection/parameters {:n n} template)]})
                 (run!))]
        (is
          (every?
            #(g/compare-to-template % template n)
            infinite-seqs))))))
