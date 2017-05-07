(ns claro.projection-benchmarks.union
  (:require [perforate.core :refer [defgoal defcase]]
            [claro.queries :refer :all]
            [claro.data :as data]
            [claro.data.ops :as ops]
            [claro.projection :as projection]
            [claro.engine :as engine]
            [manifold.deferred :as d]))

(defgoal union-projection
  "Resolution of multiple map projections with subsequent merge.")

;; ## Fixtures

(defrecord Person [id]
  data/Resolvable
  data/BatchedResolvable
  (resolve-batch! [_ _ people]
    (d/future
      (->> (map :id people)
           (fetch-people!)
           (map
             (fn [{:keys [friend-ids] :as person}]
               (assoc person :friends (map ->Person friend-ids))))))))

(def run!!
  (comp deref (engine/engine {:max-cost 1024})))

;; ## Values

(def person-with-direct-projection
  (projection/apply
    (->Person 1)
    {:id   projection/leaf
     :name projection/leaf
     :friends [{:id projection/leaf}]}))

(def person-with-single-union-projection
  (projection/apply
    (->Person 1)
    (projection/union
      {:id   projection/leaf
       :name projection/leaf
       :friends [{:id projection/leaf}]})))

(def person-with-union-projection
  (projection/apply
    (->Person 1)
    (projection/union
      {:id projection/leaf}
      {:name projection/leaf}
      {:friends [{:id projection/leaf}]})))

(letfn [(make-template [depth]
          (if (zero? depth)
            {}
            (projection/union
              {:id projection/leaf
               :name projection/leaf}
              {(projection/alias :first-friend :friends)
               (projection/prepare
                 ops/first
                 (make-template (dec depth)))})))]
  (def person-with-deep-union-projection
    (projection/apply
      (->Person 1)
      (make-template 512))))

(assert (= (run!! person-with-direct-projection)
           (run!! person-with-single-union-projection)
           (run!! person-with-union-projection)))

;; ## Cases

(defcase union-projection :direct-projection
  []
  (run!! person-with-direct-projection))

(defcase union-projection :single-element-projection
  []
  (run!! person-with-single-union-projection))

(defcase union-projection :multi-element-projection
  []
  (run!! person-with-union-projection))

(defcase union-projection :deep-projection
  []
  (run!! person-with-deep-union-projection))
