(ns claro.projection.bind
  (:refer-clojure :exclude [let])
  (:require [claro.projection.protocols :as pr]
            [claro.data.ops.then :refer [then! then]]
            [clojure.core :as core]))

;; ## Record

(defn- bind-template
  [value template bind-fn]
  (-> (pr/project template value)
      (then!
        (fn [partial-value]
          (core/let
            [template' (bind-fn partial-value)]
            (pr/project template' value))))))

(defrecord BindProjection [template bind-fn]
  pr/Projection
  (project [_ value]
    (then value #(bind-template % template bind-fn))))

(defmethod print-method BindProjection
  [^BindProjection value ^java.io.Writer w]
  (.write w "#<bind ")
  (print-method (.-template value) w)
  (.write w ">"))

;; ## Basic Constructor

(defn bind
  "A two-step projection, using a partial projection result to generate the
   eventual, full projection. Example:

   ```clojure
   (projection/bind
     (fn [{:keys [id]}]
       {:children
        [{:id        projection/leaf
          :parent-id (projection/value id)}]})
     {:id projection/leaf})
   ```

   This will use `{:id projection/leaf}` to project the current value and pass
   the result to the given function – which is then expected to return the
   \"actual\" projection template for the current value.

   This projection is useful to \"remember\" values in the tree."
  [bind-fn template]
  (->BindProjection template bind-fn))

;; ## Syntactic Sugar

(defmacro let
  "Syntactic sugar for the [[bind]] projection.

   ```clojure
   (projection/let [{:keys [id]} {:id projection/leaf}]
     {:children [{:id        projection/leaf
                  :parent-id (projection/value id)}]})
   ```

   is equal to:

   ```clojure
   (projection/bind
     (fn [{:keys [id]}]
       {:children [{:id        projection/leaf
                    :parent-id (projection/value id)}]})
     {:id projection/leaf})
   ```

   Multiple binding templates are supported (although you'll usually want to
   only use one):

   ```clojure
   (projection/let [{:keys [id]}   {:id projection/leaf}
                    {:keys [name]} {:name projection/leaf}]
     ...)
   ```
   "
  [bindings & body]
  {:pre [(seq bindings) (even? (count bindings))]}
  (if (> (count bindings) 2)
    `(let [~@(take 2 bindings)]
       (let [~@(drop 2 bindings)]
         ~@body))
    (core/let [[binding template] bindings]
      `(->BindProjection
         ~template
         (fn [~binding]
           ~@body)))))
