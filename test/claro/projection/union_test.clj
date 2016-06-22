(ns claro.projection.union-test
  (:require [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [clojure.test :refer :all]
            [claro.projection.generators :as g]
            [claro.engine.fixtures :refer [make-engine]]
            [claro.data.ops.then :refer [then]]
            [claro.projection :as projection]))

(defspec t-union-projection 100
  (let [run! (make-engine)]
    (prop/for-all
      [template (g/valid-template)
       value    (g/infinite-seq)]
      (is (= @(-> value
                  (projection/apply template)
                  (run!))
             @(-> value
                  (projection/apply
                    (projection/union
                      [(select-keys template [:next])
                       (select-keys template [:value])]))
                  (run!)))))))

(defspec t-union-projection-key-overlap 100
  (let [run! (make-engine)]
    (prop/for-all
      [template (g/valid-template)
       value    (g/infinite-seq)]
      (or (empty? template)
          (boolean
            (is
              (thrown-with-msg?
                IllegalStateException
                #"disjunct keys"
                @(-> value
                     (projection/apply (projection/union [template template]))
                     (run!)))))))))

(defspec t-conditional-union-projection 100
  (let [run! (make-engine)]
    (prop/for-all
      [template (g/valid-template)
       value    (g/non-wrapped-infinite-seq)]
      (let [value-divisible? (fn [n]
                               #(and (number? (:value %))
                                     (zero? (mod (:value %) n))))
            {:keys [n]} value]
        (is
          (= (into {}
                   [(when (zero? (mod n 3))
                      @(-> value
                           (projection/apply (select-keys template [:value]))
                           (run!)))
                    (when (zero? (mod n 5))
                      @(-> value
                           (projection/apply (select-keys template [:next]))
                           (run!)))
                    (when (zero? (mod n 15))
                      {:extra? true})])
             @(-> value
                  (then assoc :extra? true)
                  (projection/apply
                    (projection/conditional-union
                      (value-divisible? 3)  (select-keys template [:value])
                      (value-divisible? 5)  (select-keys template [:next])
                      (value-divisible? 15) {:extra? projection/leaf}))
                  (run!))))))))
