(ns claro.data.ops.then-test
  (:require [clojure.test.check :as tc]
            [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [clojure.test :refer :all]
            [claro.test :as test]
            [claro.data :as data]
            [claro.data.ops :as ops]
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
      (gen/not-empty
        (gen/one-of
          [(gen/vector g)
           (gen/set g)
           (gen/list g)
           (gen/map g g)])))
    gen-resolvable))

(deftype Preserve [v])

;; ## Tests

(defspec t-blocking-composition (test/times 50)
  (prop/for-all
    [resolvable gen-nested-resolvable]
    (let [run! (make-engine)
          value (ops/then! resolvable (juxt identity ->Preserve))
          [result preserved] @(run! value)]
      (= result (.-v preserved)))))

(defspec t-eager-composition (test/times 50)
  (prop/for-all
    [resolvable gen-nested-resolvable]
    (let [run! (make-engine)
          value (ops/then resolvable (juxt identity ->Preserve))
          [result preserved] @(run! value)
          observed (.-v preserved)]
      (and (is (not= result observed))
           (is (= result @(run! observed)))))))

(defspec t-conditional-composition (test/times 100)
  (prop/for-all
    [resolvable0 gen-resolvable
     resolvable1 gen-resolvable]
    (let [run! (make-engine)
          predicate (fn [{:keys [x y]}]
                      (and (not (instance? Identity x))
                           (not (instance? Identity y))
                           (not= (class x) (class y))))
          action (juxt :x :y)
          value (ops/on {:x resolvable0, :y resolvable1} predicate action)]
      (if (= (-> resolvable0 :v class)  (-> resolvable1 :v class))
        (boolean
          (is (thrown-with-msg?
                IllegalStateException
                #"predicate .+ does not hold for fully resolved"
                @(run! value))))
        (is (= [(:v resolvable0) (:v resolvable1)]
               @(run! value)))))))

(deftest t-composition
  (let [resolvable (->Identity "string")
        c (count (:v resolvable))
        run! (make-engine)]
    (testing "blocking composition."
      (is (= {:x c} @(run! (ops/then! {:x resolvable} update :x count)))))
    (testing "eager composition."
      (is
        (thrown-with-msg?
          Exception
          #"count not supported"
          @(run! (ops/then {:x resolvable} update :x count)))))
    (testing "conditional composition."
      (is (= {:x c} @(run! (ops/on {:x resolvable} #(-> % :x string?) update :x count)))))
    (testing "built-in update."
      (is (= {:x c} @(run! (ops/update {:x resolvable} :x count)))))))

(defspec t-nested-composition (test/times 100)
  (prop/for-all
    [resolvable gen-resolvable
     nesting-level gen/nat
     chain-fn (gen/elements [ops/then ops/then!])]
    (let [run! (make-engine)
          value (chain-fn
                  (nth (iterate ->Identity resolvable) nesting-level)
                  ->Preserve)
          result @(run! value)]
      (is (= (:v resolvable) (.-v result))))))
