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

(defspec t-conditional-composition 50
  (prop/for-all
    [resolvable0 gen-resolvable
     resolvable1 gen-resolvable]
    (let [run! (make-engine)
          predicate (fn [{:keys [x y]}]
                      (and (not (instance? Identity x))
                           (not (instance? Identity y))
                           (not= (class x) (class y))))
          action (juxt :x :y)
          value (data/on {:x resolvable0, :y resolvable1} predicate action)]
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
      (is (= {:x c} @(run! (data/then! {:x resolvable} update :x count)))))
    (testing "eager composition."
      (is (= {:x 1} @(run! (data/then {:x resolvable} update :x count)))))
    (testing "conditional composition."
      (is (= {:x c} @(run! (data/on {:x resolvable} #(-> % :x string?) update :x count)))))
    (testing "built-in update."
      (is (= {:x c} @(run! (data/update {:x resolvable} :x count)))))))
