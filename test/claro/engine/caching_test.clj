(ns claro.engine.caching-test
  (:require [clojure.test.check :as tc]
            [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [clojure.test :refer :all]
            [claro.test :as test]
            [claro.engine.fixtures :refer [make-engine]]
            [claro.data :as data]
            [claro.data.tree :as tree]))

;; ## Generators

(let [not-nan? #(not (and (number? %) (Double/isNaN %)))
      gen-any (gen/frequency
                [[10 (gen/such-that not-nan? gen/simple-type)]
                 [1 (gen/return nil)]])]
  (def gen-ephemeral-resolvable
    (gen/let [values (gen/not-empty (gen/vector gen-any))]
      (gen/return
        (let [data (atom values)]
          (reify data/Resolvable
            (resolve! [this _]
              (first (swap! data next)))))))))

(def gen-identical-resolvables
  (gen/let [resolvable     gen-ephemeral-resolvable
            nesting-depths (gen/not-empty (gen/vector gen/nat))]
    (gen/return
      (mapv
        (fn [nesting-depth]
          (reduce
            (fn [value n]
              (reify Object

                Object
                (toString [_]
                  (str (hash resolvable) ", nested: " n))

                data/Resolvable
                (resolve! [_ _]
                  value)))
            resolvable (range nesting-depth)))
        nesting-depths))))

;; ## Test

(defn- every-identical?
  [sq]
  (let [v (first sq)]
    (every? #(identical? v %) sq)))

(defspec t-caching (test/times 100)
  (let [run! (make-engine)]
    (prop/for-all
      [resolvables gen-identical-resolvables]
      (let [result (is @(run! resolvables))]
        (and (is (= (count resolvables) (count result)))
             (is (every-identical? result)))))))
