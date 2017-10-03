(ns claro.projection.error-test
  (:require [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [claro.test :as test]
            [claro.data :as data]
            [claro.projection :as projection]
            [claro.projection.generators :refer [gen-projection gen-error]]
            [claro.engine.fixtures :refer [make-engine]]))

(defspec t-projection-retains-error-values (test/times 250)
  (let [run! (make-engine)]
    (prop/for-all
      [projection gen-projection
       error      gen-error]
      (= error
         @(-> error
              (projection/apply projection)
              (run!))))))
