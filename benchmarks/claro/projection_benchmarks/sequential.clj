(ns claro.projection-benchmarks.sequential
  (:require [perforate.core :refer [defgoal defcase]]
            [claro.queries :refer :all]
            [claro.data :as data]
            [claro.data.ops :as ops]
            [claro.projection :as projection]
            [claro.engine :as engine]
            [manifold.deferred :as d]))

(defgoal sequential-projection
  "Resolution of a projection on a sequential value.")

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
  (comp deref (engine/engine)))

(def people-with-friends
  [{:id     projection/leaf
   :name    projection/leaf
   :friends [{:id projection/leaf}]}])

(def people
  (mapv ->Person (range 1 4096)))

;; ## Testcases

(let [value (projection/apply (list* people) people-with-friends)]
  (defcase sequential-projection :list
    []
    (run!! value)))

(let [value (projection/apply (vec people) people-with-friends)]
  (defcase sequential-projection :vector
    []
    (run!! value)))
