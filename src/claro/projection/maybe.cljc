(ns claro.projection.maybe
  (:require [claro.projection.protocols :as pr]
            [claro.data.ops.then :refer [then]]))

;; ## Maybe

(defrecord MaybeProjection [template]
  pr/Projection
  (project [_ value]
    (then value
          #(some->> % (pr/project template)))))

(defn maybe
  "Apply projection template if the value is not `nil`. Otherwise, just let the
   `nil` stay as it is."
  [template]
  (->MaybeProjection template))

;; ## Default

(defrecord DefaultProjection [template default-value]
  pr/Projection
  (project [_ value]
    (then value
          #(->> (if (nil? %)
                  default-value
                  %)
                (pr/project template)))))

(defn default
  "Apply the given template to the non-nil value or the given default."
  [template default-value]
  (->DefaultProjection template default-value))
