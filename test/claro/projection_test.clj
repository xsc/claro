(ns claro.projection-test
  (:require [clojure.test.check :as tc]
            [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [clojure.test :refer :all]
            [claro.projection.generators :as g]
            [claro.data :as data]
            [claro.projection :as projection]
            [claro.engine.fixtures :refer [make-engine]]))

;; ## Tests

(defn- compare-to-template
  [value template expected-value]
  (and (= (set (keys value))
          (set (keys template)))
       (or (not (contains? template :value))
           (= (:value value) expected-value))
       (or (not (contains? template :next))
           (recur (:next value) (:next template) (inc expected-value)))))

(defspec t-map-projection 200
  (let [run! (make-engine)]
    (prop/for-all
      [template (g/valid-template)
       value    (g/infinite-seq)]
      (let [projected-value (projection/apply value template)
            result @(run! projected-value)]
        (compare-to-template result template (:n value))))))

(defspec t-sequential-projection 200
  (let [run! (make-engine)]
    (prop/for-all
      [template (g/valid-template)
       values   (gen/vector (g/infinite-seq))]
      (let [projected-values (projection/apply values [template])
            results @(run! projected-values)]
        (and (vector? results)
             (empty?
               (for [[result {:keys [n]}] (map vector results values)
                     :when (not (compare-to-template result template n))]
                 result)))))))

(defspec t-set-projection 200
  (let [run! (make-engine)]
    (prop/for-all
      [template (g/valid-template)
       values   (gen/set (g/infinite-seq))]
      (let [projected-values (projection/apply values #{template})
            results @(run! projected-values)]
        (and (<= (count results) (count values))
             (set? results))))))

(defspec t-invalid-map-projection 200
  (let [run! (make-engine)]
    (prop/for-all
      [template (g/invalid-template)
       value    (g/infinite-seq)]
      (let [projected-value (projection/apply value template)]
        (boolean
          (is
            (thrown-with-msg?
              IllegalStateException
              #"can only be used for non-collection values"
              @(run! projected-value))))))))

(defspec t-map-projection-type-mismatch 200
  (let [run! (make-engine)]
    (prop/for-all
      [template (g/valid-template)
       values   (gen/vector (g/infinite-seq))]
      (let [projected-value (projection/apply values template)]
        (boolean
          (is
            (thrown-with-msg?
              IllegalArgumentException
              #"projection template is a map but value is not"
              @(run! projected-value))))))))

(defspec t-sequential-projection-type-mismatch 200
  (let [run! (make-engine)]
    (prop/for-all
      [template (g/valid-template)
       value    (g/infinite-seq)]
      (let [projected-value (projection/apply value [template])]
        (boolean
          (is
            (thrown-with-msg?
              IllegalArgumentException
              #"projection template is sequential but value is not"
              @(run! projected-value))))))))

(defspec t-set-projection-type-mismatch 200
  (let [run! (make-engine)]
    (prop/for-all
      [template (g/valid-template)
       value    (g/infinite-seq)]
      (let [projected-value (projection/apply value #{template})]
        (boolean
          (is
            (thrown-with-msg?
              IllegalArgumentException
              #"projection template is set but value is not"
              @(run! projected-value))))))))
