(ns claro.projection.sequential-test
  (:require [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [clojure.test :refer :all]
            [claro.test :as test]
            [claro.projection.generators :as g]
            [claro.engine.fixtures :refer [make-engine]]
            [claro.projection :as projection]))

(defspec t-sequential-projection (test/times 100)
  (let [run! (make-engine)]
    (prop/for-all
      [template (g/valid-template)
       values   (gen/vector (g/infinite-seq-no-mutation))]
      (let [projected-values (projection/apply values [template])
            results @(run! projected-values)]
        (and (vector? results)
             (empty?
               (for [[result {:keys [n]}] (map vector results values)
                     :when (not (g/compare-to-template result template n))]
                 result)))))))

(defspec t-sequential-projection-type-mismatch (test/times 100)
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
