(ns claro.projection.set-test
  (:require [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [clojure.test :refer :all]
            [claro.test :as test]
            [claro.projection.generators :as g]
            [claro.engine.fixtures :refer [make-engine]]
            [claro.projection :as projection]))

(defspec t-set-projection (test/times 200)
  (let [run! (make-engine)]
    (prop/for-all
      [template (g/valid-template)
       values   (let [g (g/infinite-seq)]
                  (gen/one-of
                    [(gen/vector g)
                     (gen/list g)
                     (gen/set g)]))]
      (= (->> values
              (map #(projection/apply % template))
              (map (comp deref run!))
              (into #{}))
         (-> values
             (projection/apply #{template})
             (run!)
             (deref))))))

(defspec t-set-projection-type-mismatch (test/times 200)
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
