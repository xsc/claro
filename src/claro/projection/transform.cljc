(ns claro.projection.transform
  (:require [claro.projection.protocols :as pr]
            [claro.data.tree :as tree]
            [claro.data.error :refer [with-error?]]
            [claro.data.ops
             [then :refer [then then!]]]))

;; ## Preparation (before Resolution)

(defn- apply-preparation
  [value f rest-template]
  (with-error? value
    (->> (tree/transform-partial value f)
         (pr/project rest-template))))

(defrecord Preparation [f rest-template]
  pr/Projection
  (project [_ value]
    (apply-preparation value f rest-template)))

(defmethod print-method Preparation
  [^Preparation value ^java.io.Writer w]
  (.write w "#<prepare ")
  (print-method (.-rest-template value) w)
  (.write w ">"))

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
    (with-error? value
      (-> (pr/project input-template value)
          (then! (comp #(pr/project output-template %) f))))))

(defmethod print-method Transformation
  [^Transformation value ^java.io.Writer w]
  (.write w "#<transform ")
  (print-method (.-input-template value) w)
  (.write w ">"))

(defn transform
  "A projection applying a transformation function to a fully resolved value.
   `input-template` is used to project the initial value, `output-template` will
   be used to further project the resulting value.

   For example, to extract the `:name` key from a seq of maps:

   ```clojure
   (-> [{:name \"Zebra\"}, {:name \"Tiger\"}]
       (projection/apply
         [(projection/transform :name {:name projection/leaf} projection/leaf)])
       (engine/run!!))
   ;; => [\"Zebra\" \"Tiger\"]
   ```
   "
  [f input-template output-template]
  (->Transformation f input-template output-template))
