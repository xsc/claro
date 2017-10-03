(ns claro.projection.bind-test
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
      [gen/int
       (gen/fmap ->Identity gen/int)])))

(def gen-wrapper
  (gen/elements [identity ->Identity]))

;; ## Tests

(defn bind-fn
  [vs]
  (cond (empty? vs)
        (projection/finite-value [255])

        (even? (first vs))
        (projection/transform
          (fn [vs]
            [(apply + vs)])
          [projection/leaf])

        :else
        (projection/transform
          (fn [vs]
            [(apply - vs)])
          [projection/leaf])))

(defspec t-bind (test/times 50)
  (let [run!! (comp deref (make-engine))]
    (prop/for-all
      [vs gen-vals, wf gen-wrapper]
      (let [projection (projection/bind bind-fn [projection/leaf])
            value (projection/apply (wf vs) projection)
            vs' (run!! vs)]
        (= (cond (empty? vs') [255]
                 (even? (first vs')) [(apply + vs')]
                 :else [(apply - vs')])
           (run!! value))))))

(defspec t-let (test/times 50)
  (let [run!! (comp deref (make-engine))]
    (prop/for-all
      [vs gen-vals, wf gen-wrapper]
      (let [projection (projection/let [vs [projection/leaf]]
                         (bind-fn vs))
            value (projection/apply (wf vs) projection)
            vs' (run!! vs)]
        (= (cond (empty? vs') [255]
                 (even? (first vs')) [(apply + vs')]
                 :else [(apply - vs')])
           (run!! value))))))
