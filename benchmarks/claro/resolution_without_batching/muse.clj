(ns claro.resolution-without-batching.muse
  (:require [perforate.core :refer [defcase]]
            [claro.resolution-without-batching :refer :all]
            [claro.queries :refer :all]

            ; muse namespaces
            [muse.core :as muse]
            [clojure.core.async :as async :refer [go <!]]))

;; ## Muse

(defrecord MuseImage  [id]
  muse/DataSource
  (fetch [_]
    (go (fetch-image! id))))

(defrecord MusePerson [id]
  muse/DataSource
  (fetch [_]
    (go (fetch-person! id))))

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

(defcase resolution-without-batching :muse
  "Build the desired result from (Muse) DataSources, "
  []
  (fetch-with-muse! 1))
