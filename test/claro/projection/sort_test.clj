(ns claro.projection.sort-test
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

(defspec t-sort (test/times 100)
  (let [run! (make-engine)
        run!! (comp deref run! projection/apply)]
    (prop/for-all
      [values (gen/vector
                (gen/one-of
                  [(g/infinite-seq-no-mutation)
                   (gen/let [value gen/int]
                     {:value value})]))]
      (let [sort-template (projection/sort-by
                            (projection/extract :value)
                            [{:value projection/leaf}])
            result (run!! values sort-template)]
        (= result (sort-by :value result))))))
