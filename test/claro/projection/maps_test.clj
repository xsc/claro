(ns claro.projection.maps-test
  (:require [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [clojure.test :refer :all]
            [claro.projection.generators :as g]
            [claro.engine.fixtures :refer [make-engine]]
            [claro.projection :as projection]))

(defspec t-map-projection 200
  (let [run! (make-engine)]
    (prop/for-all
      [template (g/valid-template)
       value    (g/infinite-seq)]
      (let [projected-value (projection/apply value template)
            result @(run! projected-value)]
        (g/compare-to-template result template (:n value))))))

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
