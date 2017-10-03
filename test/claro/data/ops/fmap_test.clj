(ns claro.data.ops.fmap-test
  (:require [clojure.test.check :as tc]
            [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [clojure.test :refer :all]
            [claro.test :as test]
            [claro.data :as data]
            [claro.data.ops.fmap :as ops]
            [claro.engine.fixtures :refer [make-engine]]))

;; ## Resolvable

(defrecord Identity [v]
  data/Resolvable
  (resolve! [_ _]
    v))

(def gen-nums
  (gen/not-empty
    (gen/vector
      (gen/one-of
        [gen/int
         (gen/fmap ->Identity gen/int)]))))

(def gen-op
  (gen/elements [vector + hash-set]))

;; ## Tests

(defspec t-fmap (test/times 100)
  (let [run!! (comp deref (make-engine))]
    (prop/for-all
      [nums gen-nums, op gen-op]
      (let [value (apply ops/fmap op nums)]
        (= (apply op (run!! nums)) (run!! value))))))

(defspec t-fmap-on (test/times 100)
  (let [run!! (comp deref (make-engine))]
    (prop/for-all
      [nums gen-nums, op gen-op]
      (let [value (apply ops/fmap-on #(every? number? %&) op nums)]
        (= (apply op (run!! nums)) (run!! value))))))

(defspec t-fmap! (test/times 100)
  (let [run!! (comp deref (make-engine))]
    (prop/for-all
      [nums gen-nums, op gen-op]
      (let [value (apply ops/fmap! op nums)]
        (= (apply op (run!! nums)) (run!! value))))))
