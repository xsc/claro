(ns claro.projection.transform
  (:require [claro.projection.protocols :as pr]
            [claro.data.tree :as tree]
            [claro.data.ops
             [then :refer [then]]]))

;; ## Preparation (before Resolution)

(defn- apply-preparation
  [value f rest-template]
  (->> (tree/transform-partial value f)
       (pr/project-template rest-template)))

(defrecord Preparation [f rest-template]
  pr/Projection
  (project-template [_ value]
    (apply-preparation value f rest-template)))

(defn prepare
  "A projection applying a transformation function to a value (before
   resolution!), with `rest-template` being used to further project the
   resulting value."
  [f rest-template]
  (->Preparation f rest-template))


