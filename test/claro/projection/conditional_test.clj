(ns claro.projection.conditional-test
  (:require [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [clojure.test :refer :all]
            [claro.test :as test]
            [claro.projection.generators :as g]
            [claro.engine.fixtures :refer [make-engine]]
            [claro.data.ops.then :refer [then]]
            [claro.projection :as projection]))

(defspec t-conditional-projection (test/times 100)
  (let [run! (make-engine)]
    (prop/for-all
      [template (g/valid-template)
       value    (g/non-wrapped-infinite-seq)]
      (let [value-divisible? (fn [n]
                               #(and (number? (:value %))
                                     (zero? (mod (:value %) n))))
            {:keys [n]} value]
        (is
          (= (cond (zero? (mod n 15))
                   {:extra? true}

                   (zero? (mod n 3))
                   @(-> value
                        (projection/apply (select-keys template [:value]))
                        (run!))

                   (zero? (mod n 5))
                   @(-> value
                        (projection/apply (select-keys template [:next]))
                        (run!))

                   :else {:extra? true, :value n})
             @(-> value
                  (then assoc :extra? true)
                  (projection/apply
                    (projection/conditional
                      {:value projection/leaf}
                      (value-divisible? 15) {:extra? projection/leaf}
                      (value-divisible? 3)  (select-keys template [:value])
                      (value-divisible? 5)  (select-keys template [:next])
                      :else {:extra? projection/leaf, :value projection/leaf}))
                  (run!))))))))

(defspec t-conditional-union-projection (test/times 100)
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
                      {:value projection/leaf}
                      (value-divisible? 3)  (select-keys template [:value])
                      (value-divisible? 5)  (select-keys template [:next])
                      (value-divisible? 15) {:extra? projection/leaf}))
                  (run!))))))))
