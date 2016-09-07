(ns claro.projection.transform
  (:require [claro.projection.protocols :as pr]
            [claro.data.tree :as tree]
            [claro.data.ops
             [then :refer [then then!]]]))

;; ## Preparation (before Resolution)

(defn- apply-preparation
  [value f rest-template]
  (->> (tree/transform-partial value f)
       (pr/project rest-template)))

(defrecord Preparation [f rest-template]
  pr/Projection
  (project [_ value]
    (apply-preparation value f rest-template)))

(defn prepare
  "A projection applying a transformation function to a value (before
   resolution!), with `rest-template` being used to further project the
   resulting value."
  [f rest-template]
  (->Preparation f rest-template))

;; ## Transformation (after Resolution)

(defrecord Transformation [f input-template output-template]
  pr/Projection
  (project [_ value]
    (-> (pr/project input-template value)
        (then! (comp #(pr/project output-template %) f)))))

(defn transform
  "A projection applying a transformation function to a fully resolved value.
   `input-template` is used to project the initial value, `output-template` will
   be used to further project the resulting value."
  [f input-template output-template]
  (->Transformation f input-template output-template))
