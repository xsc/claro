(ns claro.engine.multi-test
  (:require [clojure.test.check :as tc]
            [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [claro.test :as test]
            [claro.engine
             [multi :as multi]
             [fixtures :refer [make-engine]]]
            [claro.data :as data]))

;; ## Resolvable

(defrecord Inc [n]
  data/Resolvable
  (resolve! [_ {:keys [::state]}]
    (swap! state + n)))

;; ## Generators

(def gen-deltas
  (gen/vector gen/pos-int))

;; ## Tests

(defspec t-multi-engine (test/times 100)
  (let [base-engine (make-engine)
        multi-engine (multi/build base-engine)]
    (prop/for-all
      [deltas gen-deltas]
      (let [resolvables (map ->Inc deltas)]
        (= (rest (reductions + 0 deltas))
           @(multi-engine resolvables {:env {::state (atom 0)}}))))))
