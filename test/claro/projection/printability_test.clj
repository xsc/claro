(ns claro.projection.printability-test
  (:require [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [claro.test :as test]
            [claro.data :as data]
            [claro.projection :as projection]
            [claro.projection.generators :refer [gen-projection gen-error]]
            [claro.engine.fixtures :refer [make-engine]]))

(defspec t-projections-are-printable (test/times 500)
  (let [run! (make-engine)]
    (prop/for-all
      [projection gen-projection]
      (and (not (record? projection))
           (if (coll? projection)
             (string? (pr-str projection))
             (.startsWith (pr-str projection) "#<claro/"))))))

(defspec t-errors-are-printable (test/times 50)
  (let [run! (make-engine)]
    (prop/for-all
      [error gen-error]
      (.startsWith (pr-str error) "#<claro/error "))))
