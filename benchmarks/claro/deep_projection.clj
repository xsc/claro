(ns claro.deep-projection
  (:require [perforate.core :refer [defgoal defcase]]
            [claro.queries :refer :all]

            [claro.data :as data]
            [claro.data.ops :as ops]
            [claro.projection :as projection]
            [claro.engine :as engine]
            [manifold.deferred :as d]))

(defgoal deep-projection
  "Resolution of an infinite tree of resolvables, using projection.")

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

(defn- make-template
  [depth]
  (if (zero? depth)
    {}
    (projection/union
      [{:id projection/leaf
        :name projection/leaf}
       (projection/alias
         :first-friend
         :friends
         (projection/prepare
           ops/first
           (make-template (dec depth))))])))

(defn- make-value
  [id depth]
  (-> (->Person id)
      (projection/apply
        (make-template depth))))

(def run!!
  (comp deref (engine/engine {:max-batches 1024})))

;; ## Cases

(let [value (make-value 1 255)]
  (defcase deep-projection :projection-depth-256
    []
    (run!! value)))

(let [value (make-value 1 511)]
  (defcase deep-projection :projection-depth-512
    []
    (run!! value)))

(let [value (make-value 1 1023)]
  (defcase deep-projection :projection-depth-1024
    []
    (run!! value)))
