(ns claro.projection.transform
  (:require [claro.projection.protocols :as pr]
            [claro.data.tree :as tree]
            [claro.data.ops
             [then :refer [then]]]))

;; ## Transformation Logic

(defn- apply-transformation
  [value f rest-template]
  (->> (tree/transform-partial value f)
       (pr/project-template rest-template)))

(defn- apply-map-transformation
  [value f rest-template key alias-key]
  {alias-key
   (apply-transformation
     (get value key)
     f
     rest-template)})

;; ## Basic Transformation

(defrecord Transformation [f rest-template]
  pr/Projection
  (project-template [_ value]
    (apply-transformation value f rest-template)))

(defn transform
  "A projection applying a transformation function to the current value, with
   `rest-template` being used to further project the resulting value."
  [f rest-template]
  (->Transformation f rest-template))

;; ## Map Transformation

(defn- assert-map!
  [value]
  (when-not (map? value)
    (throw
      (IllegalArgumentException.
        (str "projection template is a map but value is not: "
             (pr-str value)))))
  value)

(defn- assert-contains!
  [value k]
  (when-not (contains? value k)
    (throw
      (IllegalArgumentException.
        (str "projection template expects key '" k "' but value "
             "does not contain it: " (pr-str value)))))
  value)

(defrecord MapTransformation [f rest-template key alias-key]
  pr/Projection
  (project-template [_ value]
    (->> (fn [value]
           (-> value
               (assert-map!)
               (assert-contains! key)
               (apply-map-transformation f rest-template key alias-key)))
         (then value))))

(defn transform-at
  "A projection applying a transformation function to a key within a map, with
   `rest-template` being used to further project the resulting value."
  ([key f rest-template]
   (transform-at key f rest-template key))
  ([key f rest-template alias-key]
   (if (= key alias-key)
     {key (transform f rest-template)}
     (->MapTransformation f rest-template key alias-key))))
