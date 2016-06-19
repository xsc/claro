(ns claro.projection.set-test
  (:require [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [clojure.test :refer :all]
            [claro.projection.generators :as g]
            [claro.engine.fixtures :refer [make-engine]]
            [claro.projection :as projection]))

(defspec t-set-projection 200
  (let [run! (make-engine)]
    (prop/for-all
      [template (g/valid-template)
       values   (gen/set (g/infinite-seq))]
      (let [projected-values (projection/apply values #{template})
            results @(run! projected-values)]
        (and (<= (count results) (count values))
             (set? results))))))

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
