(ns claro.resolution-without-batching.claro
  (:require [perforate.core :refer [defcase]]
            [claro.resolution-without-batching :refer :all]
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
  (resolve! [_ _]
    (d/future
      (fetch-image! id))))

(defrecord ClaroPerson [id]
  data/Resolvable
  (resolve! [_ _]
    (d/future
      (let [{:keys [image-id friend-ids] :as person} (fetch-person! id)]
        (-> person
            (assoc :friends (map ->ClaroPerson friend-ids))
            (assoc :image (->ClaroImage image-id)))))))

(defn fetch-with-claro!
  [id]
  (engine/run!!
    (-> (->ClaroPerson id)
        (ops/update
          :friends
          #(ops/map (fn [x] (dissoc x :friends)) %)))))

(defcase resolution-without-batching :claro
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

(defcase resolution-without-batching :claro-and-projection
  "Use a projection template to restrict the result tree to the desired shape."
  []
  (fetch-with-claro-and-projection! 1))
