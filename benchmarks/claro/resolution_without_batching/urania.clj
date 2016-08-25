(ns claro.resolution-without-batching.urania
  (:require [perforate.core :refer [defcase]]
            [claro.resolution-without-batching :refer :all]
            [claro.queries :refer :all]

            ; urania namespaces
            [urania.core :as u]
            [promesa.core :as prom]))

;; ## Urania

(defrecord UraniaImage  [id]
  u/DataSource
  (-identity [_] id)
  (-fetch [_ _]
    (prom/do*
      (fetch-image! id))))

(defrecord UraniaPerson [id]
  u/DataSource
  (-identity [_] id)
  (-fetch [_ _]
    (prom/do*
      (fetch-person! id))))

(defn fetch-with-urania!
  [id]
  (u/run!!
    (->> (->UraniaPerson id)
         (u/mapcat
           (fn [{:keys [friend-ids image-id] :as person}]
             (u/map
               #(assoc person :friends %1, :image %2)
               (u/traverse
                 (fn [{:keys [image-id] :as friend}]
                   (u/map #(assoc friend :image %) (->UraniaImage image-id)))
                 (u/collect (map ->UraniaPerson friend-ids)))
               (->UraniaImage image-id)))))))

(defcase resolution-without-batching :urania
  "Build the desired result from DataSources, "
  []
  (fetch-with-urania! 1))
