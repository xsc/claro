(ns claro.projection.union-test
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

(defspec t-union-projection (test/times 100)
  (let [run! (make-engine)]
    (prop/for-all
      [template (g/valid-template)
       value    (g/infinite-seq)]
      (is (= @(-> value
                  (projection/apply template)
                  (run!))
             @(-> value
                  (projection/apply
                    (projection/union*
                      [(select-keys template [:next])
                       (select-keys template [:value])]))
                  (run!)))))))

(defspec t-union-projection-key-overlap (test/times 100)
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
                     (projection/apply (projection/union* [template template]))
                     (run!)))))))))
