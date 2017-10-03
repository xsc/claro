(ns claro.data.ops.collections-test
  (:require [clojure.test.check :as tc]
            [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [clojure.test :refer :all]
            [claro.test :as test]
            [claro.data :as data]
            [claro.data.ops.collections :as ops]
            [claro.engine.fixtures :refer [make-engine]]))

;; ## Resolvable

(defrecord Identity [v]
  data/Resolvable
  (resolve! [_ _]
    v))

(def gen-wrapper
  (gen/elements
    [->Identity
     identity]))

(def gen-collection
  (gen/let [nums (gen/vector gen/int)
            num-wrappers (gen/vector gen-wrapper (count nums))
            outer-wrapper gen-wrapper]
    (gen/return
      {:nums nums
       :resolvable (outer-wrapper
                     (map #(%1 %2) num-wrappers nums))})))

;; ## Tests

(defspec t-map-with-single-collection (test/times 100)
  (let [run!! (comp deref (make-engine))]
    (prop/for-all
      [{:keys [nums resolvable]} gen-collection]
      (= (map inc nums) (run!! (ops/map inc resolvable))))))

(defspec t-map-with-multiple-collections (test/times 100)
  (let [run!! (comp deref (make-engine))]
    (prop/for-all
      [{nums1 :nums, r1 :resolvable} gen-collection
       {nums2 :nums, r2 :resolvable} gen-collection]
      (= (map + nums1 nums2) (run!! (ops/map + r1 r2))))))

(defspec t-first (test/times 50)
  (let [run!! (comp deref (make-engine))]
    (prop/for-all
      [{:keys [nums resolvable]} gen-collection]
      (= (first nums) (run!! (ops/first resolvable))))))

(defspec t-nth (test/times 50)
  (let [run!! (comp deref (make-engine))]
    (prop/for-all
      [{:keys [nums resolvable]} gen-collection
       n       gen/pos-int
       wrapper (gen/elements [identity ->Identity])]
      (let [value (ops/nth resolvable (wrapper n))]
        (if (< n (count nums))
          (= (nth nums n) (run!! value))
          (boolean
            (is
              (thrown-with-msg?
                java.lang.IllegalArgumentException
                #"index \d+ out of bounds when calling 'nth'"
                (run!! value)))))))))

(defspec t-take (test/times 50)
  (let [run!! (comp deref (make-engine))]
    (prop/for-all
      [{:keys [nums resolvable]} gen-collection
       n gen/pos-int]
      (= (take n nums) (run!! (ops/take n resolvable))))))

(defspec t-drop (test/times 50)
  (let [run!! (comp deref (make-engine))]
    (prop/for-all
      [{:keys [nums resolvable]} gen-collection
       n gen/pos-int]
      (= (drop n nums) (run!! (ops/drop n resolvable))))))
