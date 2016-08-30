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
      gen-simple  (gen/one-of
                    [gen/int
                     gen/large-integer
                     (gen/such-that not-nan? gen/double)
                     gen/char
                     gen/string-ascii
                     gen/ratio
                     gen/boolean])
      gen-any (gen/frequency
                [[10 gen-simple]
                 [1 (gen/return nil)]])]
  (def gen-ephemeral-resolvable
    (gen/let [values (gen/not-empty (gen/vector gen-any))]
      (gen/return
        (let [data (atom values)]
          (reify data/Resolvable
            (resolve! [this _]
              (first (swap! data next)))))))))

(def gen-nesting-depths
  (gen/not-empty (gen/vector (gen/fmap #(min 256 %) gen/nat))))

(def gen-identical-resolvables
  (gen/let [resolvable     gen-ephemeral-resolvable
            nesting-depths gen-nesting-depths]
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
  (let [run! (make-engine {:max-batches Long/MAX_VALUE})]
    (prop/for-all
      [resolvables gen-identical-resolvables]
      (let [result (is @(run! resolvables))]
        (and (is (= (count resolvables) (count result)))
             (is (every-identical? result)))))))
