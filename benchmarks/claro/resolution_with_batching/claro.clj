(ns claro.resolution-with-batching.claro
  (:require [perforate.core :refer [defcase]]
            [claro.resolution-with-batching :refer :all]
            [claro.queries :refer :all]

            ;; claro namespaces
            [claro.data :as data]
            [claro.data.ops :as ops]
            [claro.projection :as projection]
            [claro.engine :as engine]
            [manifold.deferred :as d]))

;; ## Claro

(defrecord ClaroImage [id]
  data/Resolvable
  data/BatchedResolvable
  (resolve-batch! [_ _ images]
    (d/future
      (fetch-images! (map :id images)))))

(defrecord ClaroPerson [id]
  data/Resolvable
  data/BatchedResolvable
  (resolve-batch! [_ _ people]
    (d/future
      (->> (map :id people)
           (fetch-people!)
           (map
             (fn [{:keys [image-id friend-ids] :as person} ]
               (-> person
                   (assoc :friends (map ->ClaroPerson friend-ids))
                   (assoc :image (->ClaroImage image-id)))))))))

(defn fetch-with-claro!
  [id]
  (engine/run!!
    (-> (->ClaroPerson id)
        (ops/update
          :friends
          #(ops/map (fn [x] (dissoc x :friends)) %)))))

(defcase resolution-with-batching :claro
  "Explicitly remove the nested ':friends' key to avoid infinite expansion."
  []
  (fetch-with-claro! 1))

;; ## Claro w/ Projection

(defn fetch-with-claro-and-projection!
  [id]
  (engine/run!!
    (-> (->ClaroPerson id)
        (projection/apply
          {:id         projection/leaf
           :name       projection/leaf
           :image-id   projection/leaf
           :friend-ids [projection/leaf]
           :image      projection/leaf
           :friends    [{:id projection/leaf
                         :name projection/leaf
                         :image-id projection/leaf
                         :friend-ids [projection/leaf]
                         :image projection/leaf}]}))))

(defcase resolution-with-batching :claro-and-projection
  "Use a projection template to restrict the result tree to the desired shape."
  []
  (fetch-with-claro-and-projection! 1))
