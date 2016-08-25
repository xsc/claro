(ns claro.resolution-with-batching.muse
  (:require [perforate.core :refer [defcase]]
            [claro.resolution-with-batching :refer :all]
            [claro.queries :refer :all]

            ; muse namespaces
            [muse.core :as muse]
            [cats.core :as cats]
            [clojure.core.async :as async :refer [go <!]]))

;; ## Muse

(defrecord MuseImage  [id]
  muse/DataSource
  (fetch [_]
    (go (fetch-image! id)))

  muse/BatchedSource
  (fetch-multi [_ images]
    (go
      (let [ids (cons id (map :id images))]
        (zipmap ids (fetch-images! ids))))))

(defrecord MusePerson [id]
  muse/DataSource
  (fetch [_]
    (go (fetch-person! id)))

  muse/BatchedSource
  (fetch-multi [_ people]
    (go
      (let [ids (cons id (map :id people))]
        (zipmap ids (fetch-people! ids))))))

(defn fetch-with-muse!
  [id]
  (muse/run!!
    (->> (->MusePerson id)
         (muse/flat-map
           (fn [{:keys [friend-ids image-id] :as person}]
             (muse/fmap
               #(assoc person :friends %1, :image %2)
               (muse/traverse
                 (fn [{:keys [image-id] :as friend}]
                   (muse/fmap #(assoc friend :image %) (->MuseImage image-id)))
                 (muse/collect (map ->MusePerson friend-ids)))
               (->MuseImage image-id)))))))

(defcase resolution-with-batching :muse
  "Build the desired result from (Muse) DataSources, "
  []
  (fetch-with-muse! 1))
