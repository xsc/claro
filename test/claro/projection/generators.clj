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
