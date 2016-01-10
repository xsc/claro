(ns claro.engine.projection-test
  (:require [clojure.test.check :as tc]
            [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [clojure.test :refer :all]
            [claro.data :as data]
            [claro.engine.fixtures :refer [make-engine]]))

(defrecord InfiniteSeq [n]
  data/Resolvable
  (resolve! [_ _]
    {:value n
     :next (InfiniteSeq. (inc n))}))

(defspec t-map-projection 100
  (prop/for-all
    [start-n gen/int
     length  gen/nat]
    (let [run! (make-engine (atom []))
          path (concat (repeat length :next) [:value])
          projection-template (assoc-in {:value nil} path nil)
          value (data/project (InfiniteSeq. start-n) projection-template)
          result @(run! value)]
      (and (is (= start-n (:value result)))
           (is (= (+ start-n length) (get-in result path)))))))

(defspec t-sequential-projection 50
  (prop/for-all
    [seq-length gen/nat
     start-n    gen/int
     length     gen/nat]
    (let [run! (make-engine (atom []))
          path (concat (repeat length :next) [:value])
          projection-template [(assoc-in {:value nil} path nil)]
          value (data/project
                  (repeat seq-length (InfiniteSeq. start-n))
                  projection-template)
          result @(run! value)]
      (and (is (= seq-length (count result)))
           (is (every? #(= (:value %) start-n) result))
           (is (every? #(=  (get-in % path) (+ start-n length)) result))))))
