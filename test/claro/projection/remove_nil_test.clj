(ns claro.projection.remove-nil-test
  (:require [clojure.test.check :as tc]
            [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [clojure.test :refer :all]
            [claro.test :as test]
            [claro.data :as data]
            [claro.projection :as projection]
            [claro.engine.fixtures :refer [make-engine]]))

;; ## Resolvable

(defrecord Identity [v]
  data/Resolvable
  (resolve! [_ _]
    v))

(def gen-vals
  (gen/vector
    (gen/one-of
      [gen/simple-type-printable
       (gen/return nil)
       (gen/fmap ->Identity gen/int)])))

(def gen-wrapper
  (gen/elements [identity ->Identity]))

;; ## Tests

(defspec t-remove-nil-elements (test/times 50)
  (let [run!! (comp deref (make-engine))]
    (prop/for-all
      [vs gen-vals
       wf gen-wrapper]
      (let [value (projection/apply
                    (wf vs)
                    (projection/remove-nil-elements))]
        (= (remove nil? (run!! vs))
           (run!! value))))))

(defspec t-remove-nil-elements-with-template (test/times 50)
  (let [run!! (comp deref (make-engine))]
    (prop/for-all
      [vs (gen/fmap
            (fn [vs]
              (mapv #(some->> % (hash-map :x 1, :v)) vs))
            gen-vals)
       wf gen-wrapper]
      (let [value (projection/apply
                    (wf vs)
                    (projection/remove-nil-elements
                      [{:v projection/leaf}]))]
        (= (->> (run!! vs)
                (remove nil?)
                (mapv #(select-keys % [:v])))
           (run!! value))))))
