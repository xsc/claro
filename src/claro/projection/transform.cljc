(ns claro.projection.transform
  (:require [claro.projection.protocols :as pr]
            [claro.data.tree :as tree]
            [claro.data.error :refer [with-error?]]
            [claro.data.ops.chain :as chain]))

;; ## Preparation (before Resolution)

(defn- apply-preparation
  [value f rest-template]
  (with-error? value
    (->> (tree/transform-partial value f)
         (pr/project rest-template))))

(deftype Preparation [f rest-template]
  pr/Projection
  (project [_ value]
    (apply-preparation value f rest-template)))

(defmethod print-method Preparation
  [^Preparation value ^java.io.Writer w]
  (.write w "#<claro/prepare ")
  (print-method (.-rest-template value) w)
  (.write w ">"))

(defn prepare
  "A projection applying a transformation function to a value (before
   resolution!), with `rest-template` being used to further project the
   resulting value."
  [f rest-template]
  (->Preparation f rest-template))

;; ## Transformation (after Resolution)

(deftype Transformation [f input-template output-template]
  pr/Projection
  (project [_ value]
    (with-error? value
      (-> (pr/project input-template value)
          (chain/chain-blocking
            (if output-template
              (comp #(pr/project output-template %) f)
              f))))))

(defmethod print-method Transformation
  [^Transformation value ^java.io.Writer w]
  (.write w "#<claro/transform ")
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

   If no `output-template` is given, you _have_ to apply projections to
   potentially infinite subtrees within the transformation function.

   If the transformation won't introduce any new resolvables,
   [[transform-finite]] should be preferred due to its better performance with
   deeply nested trees."
  ([f input-template]
   (->Transformation f input-template nil))
  ([f input-template output-template]
  (->Transformation f input-template output-template)))

;; ## Transformation to Finite Value

(deftype FiniteTransformation [f input-template]
  pr/Projection
  (project [_ value]
    (with-error? value
      (-> (pr/project input-template value)
          (chain/chain-blocking* f)))))

(defmethod print-method FiniteTransformation
  [^FiniteTransformation value ^java.io.Writer w]
  (.write w "#<claro/transform ")
  (print-method (.-input-template value) w)
  (.write w ">"))

(defn ^{:added "0.2.15"} transform-finite
  "Like [[transform]] but assuming that `f` produces a finite value, i.e.
   one without any further resolvables.

   For transformations on deeply nested structures this will perform better
   than [[transform]] since it avoids re-inspection of the tree."
  [f input-template]
  (->FiniteTransformation f input-template))
