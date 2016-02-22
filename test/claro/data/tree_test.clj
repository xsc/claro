(ns claro.data.tree-test
  (:require [clojure.test.check :as tc]
            [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [clojure.test :refer :all]
            [clojure.set :as set]
            [claro.data.protocols :as p]
            [claro.data.tree :as tree]))

;; ## Generator

(defrecord R [x]
  p/Resolvable)

(def gen-resolvables
  (gen/fmap
    (fn [length]
      (set (repeatedly length #(->R (rand-int 1000)))))
    gen/s-pos-int))

(defn gen-resolvable-tree
  [rs]
  (gen/recursive-gen
    (fn [g]
      (gen/one-of
        [(gen/list g)
         (gen/set g)
         (gen/vector g)
         (gen/map g g)]))
    (gen/one-of [(gen/elements rs) gen/string-ascii])))

(def gen-tree
  (->> (fn [rs]
         (gen/tuple
           (gen/return rs)
           (gen-resolvable-tree rs)))
       (gen/bind gen-resolvables)
       (gen/fmap
         (fn [[rs tree]]
           [rs (tree/wrap-tree tree)]))))

(def gen-collection
  (->> (gen/elements [[] () #{}])
       (gen/tuple gen-resolvables)
       (gen/fmap
         (fn [[rs empty-coll]]
           [rs (into empty-coll rs)]))))

;; ## Helper

(defn- ->resolution
  [resolvables]
  (into {} (map (juxt identity :x) resolvables)))

(defn- ->partial-resolution
  [resolvables ratio]
  (->> #(rand-nth (seq resolvables))
       (repeatedly (Math/floor (* (count resolvables) ratio)))
       (->resolution)))

;; ## Resolution

(defn- attempt-resolution
  [tree resolvables]
  (or (empty? resolvables)
      (let [resolvable->resolved (->partial-resolution resolvables 0.5)
            resolved (set (keys resolvable->resolved))
            tree' (p/apply-resolved-values tree resolvable->resolved)
            rs (into #{} (p/resolvables tree'))]
        (and (is (not-any? resolved rs))
             (is (= rs (set/difference (set resolvables) resolved)))))))

(defspec t-tree 200
  (prop/for-all
    [[available-resolvables tree] gen-tree]
    (let [rs (p/resolvables tree)]
      (and (is (every? p/resolvable? rs))
           (is (every? #(contains? available-resolvables %) rs))
           (attempt-resolution tree rs)))))

;; ## Collections

(defspec t-collections 100
  (prop/for-all
    [[available-resolvables coll] gen-collection]
    (let [resolvable->value (->resolution available-resolvables)
          coll-tree (tree/wrap-tree coll)
          result (p/apply-resolved-values coll-tree resolvable->value)]
      (and (is (p/resolved? result))
           (is (= (class coll) (class result)))
           (or (not (sequential? coll)) (is (= (map :x coll) result)))))))
