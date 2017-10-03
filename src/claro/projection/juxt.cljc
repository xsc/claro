(ns claro.projection.juxt
  (:refer-clojure :exclude [juxt])
  (:require [claro.projection.protocols :as pr]
            [claro.data.error :refer [with-error?]]
            [claro.data.ops.chain :as chain]))

;; ## Record

(deftype JuxtProjection [templates]
  pr/Projection
  (project [_ value]
    (with-error? value
      (chain/chain-eager
        value
        (fn [result]
          (with-error? result
            (mapv #(pr/project % result) templates)))))))

(defmethod print-method JuxtProjection
  [^JuxtProjection value ^java.io.Writer w]
  (.write w "#<claro/juxt ")
  (print-method (vec (.-templates value)) w)
  (.write w ">"))

;; ## Constructor

(defn ^{:added "0.2.13"} juxt*
  "Creates a vector with results of projecting the current value with each
   of the given `templates` (maintaining order):

   ```clojure
   (projection/juxt*
     [(projection/extract :id)
      (projection/extract :name)])
   ```

   This, for example, will convert a map with `:id` and `:name` keys to a
   tuple."
  [templates]
  {:pre [(seq templates)]}
  (->JuxtProjection templates))

(defn ^{:added "0.2.13"} juxt
  "Creates a vector with results of projecting the current value with each
   of the given `templates` (maintaining order):

   ```clojure
   (projection/juxt
     (projection/extract :id)
     (projection/extract :name))
   ```

   This, for example, will convert a map with `:id` and `:name` keys to a
   tuple."
  [& templates]
  (juxt* templates))
