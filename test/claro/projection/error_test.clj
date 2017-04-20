(ns claro.projection.error-test
  (:require [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [claro.test :as test]
            [claro.data :as data]
            [claro.projection :as projection]
            [claro.engine.fixtures :refer [make-engine]]))

(defn key-set
  [v]
  (if (map? v)
    (set (keys v))))

(defn gen-distinct
  [g]
  (gen/such-that
    #(or (not-any? map? %)
         (empty?
           (clojure.set/intersection
             (key-set (first %))
             (key-set (second %)))))
    (gen/vector g 2)))

(def gen-projection
  (->> (gen/recursive-gen
         (fn [g]
           (gen/one-of
             [(gen/fmap vector g)
              (gen/fmap hash-set g)
              (gen/let [v gen/string-alpha-numeric]
                (gen/hash-map v g))

              (gen/let [v gen/string-alpha-numeric]
                (gen/hash-map (projection/alias v :nested) g))
              (gen/fmap projection/maybe g)
              (gen/fmap projection/extract gen/keyword)
              (gen/fmap #(projection/juxt % %) g)
              (gen/fmap #(projection/transform identity % %) g)
              (gen/fmap #(projection/prepare identity %) g)
              (gen/fmap #(projection/parameters {} %) g)
              (gen/fmap #(projection/conditional % some? %) g)
              (gen/fmap #(projection/case Object %) g)
              (gen/fmap #(projection/case-resolvable Object %) g)
              (gen/fmap projection/union* (gen-distinct g))]))
         (gen/elements
           [(projection/default projection/leaf ::default)
            (projection/levels 1)
            (projection/value ::value)
            (projection/value {:a 1} {:a projection/leaf})
            (projection/finite-value ::value)
            projection/leaf]))))

(def gen-error
  (->> (gen/tuple
         gen/string-ascii
         (gen/one-of
           [(gen/return nil)
            (gen/map gen/string-ascii gen/string-ascii)]))
       (gen/fmap #(apply data/error %))))

(defspec t-projection-retains-error-values (test/times 250)
  (let [run! (make-engine)]
    (prop/for-all
      [projection gen-projection
       error      gen-error]
      (= error
         @(-> error
              (projection/apply projection)
              (run!))))))
