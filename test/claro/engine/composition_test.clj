(ns claro.engine.composition-test
  (:require [clojure.test.check :as tc]
            [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [clojure.test :refer :all]
            [claro.data :as data]
            [claro.engine.fixtures :refer [make-engine]]))

;; ## Generator

(defrecord Identity [v]
  data/Resolvable
  (resolve! [_ _]
    v))

(def gen-resolvable
  (gen/fmap ->Identity gen/simple-type))

(def gen-nested-resolvable
  (gen/recursive-gen
    (fn [g]
      (gen/one-of
        [(gen/vector g)
         (gen/set g)
         (gen/list g)
         (gen/map g g)]))
    gen-resolvable))

;; ## Tests

(defspec t-blocking-composition 50
  (prop/for-all
    [resolvable gen-nested-resolvable]
    (let [run! (make-engine)
          value (data/then! resolvable (juxt identity pr-str))
          [result printed] @(run! value)]
      (= (pr-str result) printed))))

(defspec t-eager-composition 50
  (prop/for-all
    [resolvable gen-nested-resolvable]
    (let [run! (make-engine)
          value (data/then resolvable (juxt identity pr-str))
          [result printed] @(run! value)]
      (= (pr-str resolvable) printed))))
