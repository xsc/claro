(ns claro.runtime.impls-test
  (:require [clojure.test :refer :all]
            [claro.runtime.impl :as impl]
            [claro.runtime.impl
             [core-async :as core-async]
             #?(:clj [manifold :as manifold])]))

;; ## Test Harness

(defn test-impl
  [impl]
  (let [impl (is (impl/->deferred-impl impl))
        chain #(apply impl/chain impl %&)
        zip #(impl/zip impl %)
        value #(impl/value impl %)
        run #(impl/run impl %)
        loop #(impl/loop impl %1 %2)
        recur #(impl/recur impl %)
        deferred? #(impl/deferred? impl %)]
    (testing "values."
      (let [d (is (value :value))]
        (is (deferred? d))
        (is (impl/deferrable? impl d))
        (is (= d (impl/->deferred impl d)))))
    (testing "run."
      (let [d (run (constantly :value))]
        (is (deferred? d))
        (is (impl/deferrable? impl d))
        (is (= d (impl/->deferred impl d)))
        (chain d (fn [value] (is (= :value value))))))
    (testing "chain."
      (is (deferred?
            (chain
              (value 0)
              inc
              inc
              (fn [v] (is (= v 2)))))))
    (testing "chain w/ intermediate deferreds."
      (is (deferred?
            (chain
              (value 0)
              #(value (+ % 2))
              (fn [v] (is (= v 2)))))))
    (testing "zip."
      (let [z (zip [])]
        (is (deferred? z))
        (chain z (fn [v] (is (= [] v)))))
      (let [z (zip [(value 0) (value 1) (value 2)])]
        (is (deferred? z))
        (chain z (fn [v] (is (= [0 1 2] v))))))
    (testing "loop/recur."
      (letfn [(inc-step [x]
                (if (< x 10)
                  (chain
                    (value (inc x))
                    recur)
                  (value x)))]
        (let [l (loop inc-step 0)]
          (is (deferred? l))
          (chain l (fn [v] (is (= 10 v)))))))))

;; ## Tests (Clojure)

#?(:clj
    (deftest t-manifold
      (test-impl manifold/impl)))

;; ## Tests (Clojure + ClojureScript)
(deftest t-core-async
  (test-impl core-async/impl))
