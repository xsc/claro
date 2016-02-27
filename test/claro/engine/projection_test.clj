(ns claro.engine.projection-test
  (:require [clojure.test.check :as tc]
            [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [clojure.test :refer :all]
            [claro.data :as data]
            [claro.engine.fixtures :refer [make-engine]]))

;; ## Fixtures

(defrecord InfiniteSeq [n]
  data/Resolvable
  (resolve! [_ _]
    {:value n
     :next (InfiniteSeq. (inc n))}))

;; ## Generators

(defn gen-infinite-seq
  []
  (gen/fmap ->InfiniteSeq gen/int))

(defn- gen-leaf-template
  []
  (gen/one-of
    [(gen/return nil)
     (gen/fmap
       #(hash-map
          :leaf?    true
          :valid?   false
          :depth    0
          :template %)
       (gen/elements [nil data/leaf]))]))

(defn- valid-next-template?
  [t]
  (or (nil? t)
      (and (:valid? t) (not (:leaf? t)))))

(defn- assoc-template
  [t k v]
  (if v
    (assoc t k (:template v))
    t))

(defn gen-template
  [valid?]
  (->> (gen/recursive-gen
         (fn [template-gen]
           (gen/let [value-template (gen-leaf-template)
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
         (gen-leaf-template))
       (gen/fmap #(dissoc % :leaf?))
       (#(gen/such-that
           (if valid? :valid? (complement :valid?))
           %
           50))
       (gen/fmap :template)))

(defn- gen-valid-template
  []
  (gen-template true))

(defn- gen-invalid-template
  []
  (gen-template false))

;; ## Tests

(defn- compare-to-template
  [value template expected-value]
  (and (= (set (keys value))
          (set (keys template)))
       (or (not (contains? template :value))
           (= (:value value) expected-value))
       (or (not (contains? template :next))
           (recur (:next value) (:next template) (inc expected-value)))))

(defspec t-map-projection 200
  (let [run! (make-engine)]
    (prop/for-all
      [template (gen-valid-template)
       value    (gen-infinite-seq)]
      (let [projected-value (data/project value template)
            result @(run! projected-value)]
        (compare-to-template result template (:n value))))))

(defspec t-sequential-projection 200
  (let [run! (make-engine)]
    (prop/for-all
      [template (gen-valid-template)
       values   (gen/vector (gen-infinite-seq))]
      (let [projected-values (data/project values [template])
            results @(run! projected-values)]
        (empty?
          (for [[result {:keys [n]}] (map vector results values)
                :when (not (compare-to-template result template n))]
            result))))))

(defspec t-invalid-map-projection 200
  (let [run! (make-engine)]
    (prop/for-all
      [template (gen-invalid-template)
       value    (gen-infinite-seq)]
      (let [projected-value (data/project value template)]
        (boolean
          (is
            (thrown-with-msg?
              IllegalStateException
              #"can only be used for non-collection values"
              @(run! projected-value))))))))
