(ns claro.engine.blocking-composition-test
  (:require [clojure.test.check :as tc]
            [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [clojure.test :refer :all]
            [claro.data :as data]
            [claro.engine.fixtures :refer [make-engine]]))

(defrecord Identity [v]
  data/Resolvable
  (resolve! [_ _]
    v))

(defspec t-blocking-composition 100
  (prop/for-all
    [v0  gen/int
     v1  (gen/such-that #(not= % 0) gen/int)
     op (gen/elements [+ - * /])
     n  gen/s-pos-int]
    (let [run! (make-engine (atom []))
          base (nth (iterate #(Identity. %) v0) n)
          value (data/then base #(op % v1))]
      (= (op v0 v1) @(run! value)))))
