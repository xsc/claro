(ns claro.projection.generators
  (:require [clojure.test.check.generators :as gen]
            [claro.data :as data]
            [claro.projection :as projection]))

;; ## Fixtures

(defrecord Identity [value]
  data/Resolvable
  (resolve! [_ _]
    value))

(defrecord InfiniteSeq [n]
  data/Resolvable
  (resolve! [_ _]
    {:value n
     :next  (->InfiniteSeq (inc n))}))

(defrecord WrappedInfiniteSeq [n]
  data/Resolvable
  (resolve! [_ _]
    {:value (->Identity n)
     :next  (->WrappedInfiniteSeq (inc n))}))

(defrecord InfiniteSeqMutation [n]
  data/Mutation
  data/Resolvable
  (resolve! [_ _]
    (->InfiniteSeq n)))

;; ## Generators

(defn infinite-seq-no-mutation
  []
  (gen/let [start-n     gen/int
            constructor (gen/elements
                          [->InfiniteSeq
                           ->WrappedInfiniteSeq])]
    (gen/return (constructor start-n))))

(defn infinite-seq
  []
  (gen/let [start-n     gen/int
            constructor (gen/elements
                          [->InfiniteSeq
                           ->WrappedInfiniteSeq
                           ->InfiniteSeqMutation])]
    (gen/return (constructor start-n))))

(defn non-wrapped-infinite-seq
  []
  (gen/fmap ->InfiniteSeq gen/int))

(defn- leaf-template
  []
  (gen/one-of
    [(gen/return nil)
     (gen/fmap
       #(hash-map
          :leaf?    true
          :valid?   false
          :depth    0
          :template %)
       (gen/elements [nil projection/leaf]))]))

(defn- valid-next-template?
  [t]
  (or (nil? t)
      (and (:valid? t) (not (:leaf? t)))))

(defn- assoc-template
  [t k v]
  (if v
    (assoc t k (:template v))
    t))

(defn raw-template
  [valid?]
  (->> (gen/recursive-gen
         (fn [template-gen]
           (gen/let [value-template (leaf-template)
                     next-template  template-gen]
             (let [new-depth ((fnil inc 0) (:depth next-template))]
               (gen/return
                 {:leaf?    false
                  :valid?   (and (< new-depth 256)
                                 (valid-next-template? next-template))
                  :depth    new-depth
                  :template (-> {}
                                (assoc-template :value value-template)
                                (assoc-template :next next-template))}))))
         (leaf-template))
       (gen/fmap #(dissoc % :leaf?))
       (#(gen/such-that
           (if valid? :valid? (complement :valid?))
           %
           50))))

(defn template
  [valid?]
  (gen/fmap :template (raw-template valid?)))

(defn valid-template
  []
  (template true))

(defn invalid-template
  []
  (template false))

(defn compare-to-template
  [value template expected-value]
  (and (= (set (keys value))
          (set (keys template)))
       (or (not (contains? template :value))
           (= (:value value) expected-value))
       (or (not (contains? template :next))
           (recur (:next value) (:next template) (inc expected-value)))))

(defn key-set
  [v]
  (if (map? v)
    (set (keys v))))

;; ## Projection Generator

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
  (let [leaf-gen (gen/elements
                   [(projection/default projection/leaf ::default)
                    (projection/levels 1)
                    (projection/value ::value)
                    (projection/value {:a 1} {:a projection/leaf})
                    (projection/finite-value ::value)
                    projection/leaf
                    projection/unsafe])]
    (gen/one-of
      [(gen/recursive-gen
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
              (gen/fmap #(projection/transform-finite identity %) g)
              (gen/fmap #(projection/prepare identity %) g)
              (gen/fmap #(projection/parameters {} %) g)
              (gen/fmap #(projection/maybe-parameters {} %) g)
              (gen/fmap #(projection/conditional % some? %) g)
              (gen/fmap #(projection/case Object %) g)
              (gen/fmap #(projection/case-resolvable Object %) g)
              (gen/fmap #(projection/sort-by % %) g)
              (gen/fmap #(projection/bind (constantly %) %) g)
              (gen/fmap projection/remove-nil-elements g)
              (gen/fmap projection/union* (gen-distinct g))
              (gen/fmap projection/merge* (gen-distinct g))]))
         leaf-gen)
       leaf-gen])))

(def gen-error
  (->> (gen/tuple
         gen/string-ascii
         (gen/one-of
           [(gen/return nil)
            (gen/map gen/string-ascii gen/string-ascii)]))
       (gen/fmap #(apply data/error %))))
