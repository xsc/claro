(ns claro.projection.maybe
  (:require [claro.projection.protocols :as pr]
            [claro.projection.maps :as maps]
            [claro.data.ops.then :refer [then]]))

;; ## Maybe

(defrecord MaybeProjection [template]
  pr/Projection
  (project [_ value]
    (then value
          #(some->> % (pr/project template))))

  maps/MapValueProjection
  (project-value [this value]
    (pr/project this value))
  (project-missing-value [this _]
    nil))

(defn maybe
  "Apply projection template if the value is not `nil`, otherwise just keep the
   `nil`.

   ```clojure
   (projection/maybe {:name projection/leaf})
   ```

   Note that this will cause a `nil` to be injected into a result map even if
   the respective key is missing."
  [template]
  (->MaybeProjection template))

(defmethod print-method MaybeProjection
  [^MaybeProjection value ^java.io.Writer w]
  (.write w "#<claro/maybe ")
  (print-method (.-template value) w)
  (.write w ">"))

;; ## Default

(defrecord DefaultProjection [template default-value]
  pr/Projection
  (project [_ value]
    (then value
          #(->> (if (nil? %)
                  default-value
                  %)
                (pr/project template))))

  maps/MapValueProjection
  (project-value [this value]
    (pr/project this value))
  (project-missing-value [this _]
    (pr/project template default-value)))

(defn default
  "Apply the given projection to any non-nil value or the given default.

   ```clojure
   (projection/default {:name projection/leaf} unknown-person)
   ```

   Note that this will cause the default value to be injected into a result
   map even if the respective key is missing."
  [template default-value]
  (->DefaultProjection template default-value))

(defmethod print-method DefaultProjection
  [^DefaultProjection value ^java.io.Writer w]
  (.write w "#<claro/default ")
  (print-method (.-template value) w)
  (.write w " | ")
  (print-method (.-default-value value) w)
  (.write w ">"))
