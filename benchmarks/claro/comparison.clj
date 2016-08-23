(ns claro.comparison
  (:require [perforate.core :refer [defgoal defcase]]

            ;; claro namespaces
            [claro.data :as data]
            [claro.data.ops :as ops]
            [claro.projection :as projection]
            [claro.engine :as engine]
            [manifold.deferred :as d]

            ; urania namespaces
            [urania.core :as u]
            [promesa.core :as prom]))

;; ## Testcase
;;
;; We'll model a three-level resolution for all candidates:
;; - resolve Person with :image-id and :friend-ids,
;; - resolve :friend-ids as a seq of [Person]
;; - resolve :image-id into an Image.

(defgoal resolution-without-batching
  "Resolution of a finite tree of Resolvables (without batching).")

;; ## Query Functions

(def ^:private sleep
  (if-let [ms (some-> (System/getenv "CLARO_BENCHMARK_LATENCY") (Long.))]
    #(Thread/sleep ms)
    (constantly nil)))

(defn fetch-person!
  [id]
  (sleep)
  {:id        id
   :name      (str "Person #" id)
   :image-id   (* id 300)
   :friend-ids (range (inc id) (* id 10) (* 3 id))})

(defn fetch-image!
  [image-id]
  (sleep)
  (str "http://images.claro.de/" image-id ".png"))

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

(defn- fetch-with-claro!
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

(defn- fetch-with-claro-and-projection!
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

(assert
  (= (fetch-with-urania! 1)
     (fetch-with-claro! 1)
     (fetch-with-claro-and-projection! 1))
  "Results of Testcases do not match!")
