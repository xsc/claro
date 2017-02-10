(ns claro.data.transform-test
  (:require [clojure.test.check
             [properties :as prop]
             [generators :as gen]
             [clojure-test :refer [defspec]]]
            [claro.engine :as engine]
            [claro.data :as data]))

;; ## Resolvables

(defrecord IncResolvable [v]
  data/BatchedResolvable
  (resolve-batch! [_ _ vs]
    (map (comp inc :v) vs)))

(defrecord RangeResolvable [n]
  data/Resolvable
  (resolve! [_ _]
    (range n)))

(defrecord OtherRangeResolvable [n]
  data/Resolvable
  (resolve! [_ _]
    (range n)))

(defrecord Point [x y z]
  data/Resolvable
  (resolve! [this _]
    (into {} this)))

(defrecord OtherPoint [x y z]
  data/Resolvable
  (resolve! [this _]
    (into {} this)))

;; ## Transform

(data/extend-list-transform
  RangeResolvable
  [->IncResolvable]

  OtherRangeResolvable
  [inc])

(data/extend-transform
  Point
  {:x'  [inc :x]
   :y'  :y
   :sum (fn [{:keys [x y z]}]
          (+ x y z))}

  OtherPoint
  {:x'  [->IncResolvable :x]
   :y'  :y
   :sum [+ :x :y :z]})

;; ## Tests

(defspec t-extend-list-transform 100
  (prop/for-all
    [n gen/s-pos-int
     f (gen/elements [->RangeResolvable ->OtherRangeResolvable])]
    (= (map inc (range n))
       (engine/run!! (f n)))))

(defspec t-extend-transform 100
  (prop/for-all
    [x gen/int
     y gen/int
     z gen/int
     f (gen/elements [->Point ->OtherPoint])]
    (= {:x x, :y y, :z z, :sum (+ x y z), :x' (inc x), :y' y}
       (engine/run!! (f x y z)))))
