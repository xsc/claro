(ns claro.projection.parameters
  (:require [claro.data.protocols :as p]
            [claro.data.tree :as tree]
            [claro.data.error :refer [with-error?]]
            [claro.projection.protocols :as pr]))

(defn- assert-resolvable!
  [value params]
  (when-not (p/resolvable? value)
    (throw
      (IllegalArgumentException.
        (str
          "'parameters' projection requires a resolvable.\n"
          "parameters: " (pr-str params) "\n"
          "value:      " (pr-str value)))))
  value)

(defn- assert-allowed-params!
  [value params]
  (doseq [[k v] params
          :let [current (get value k ::none)]]
    (when (= current ::none)
      (throw
        (IllegalArgumentException.
          (format
            (str "'parameters' projection requires key '%s' to exist.%n"
                 "parameters: %s%n"
                 "value:      %s")
            k
            (pr-str params)
            (pr-str value)))))
    (when-not (nil? current)
      (throw
        (IllegalArgumentException.
          (format
            (str "'parameters' projection cannot override non-nil value "
                 "at key '%s'%n"
                 "parameters: %s%n"
                 "value:      %s")
            k
            (pr-str params)
            (pr-str value))))))
  value)

(defn- inject-params
  [value params]
  (-> value
      (assert-resolvable! params)
      (assert-allowed-params! params)
      (p/set-parameters params)))

(defrecord ParametersProjection [params rest-template]
  pr/Projection
  (project [_ value]
    (with-error? value
      (->> #(inject-params % params)
           (tree/transform-partial value)
           (pr/project rest-template)))))

(defmethod print-method ParametersProjection
  [^ParametersProjection value ^java.io.Writer w]
  (.write w "#<claro/parameters ")
  (print-method (.-params value) w)
  (.write w " => ")
  (print-method (.-rest-template value) w)
  (.write w ">"))

(defn parameters
  "Set some fields within a Resolvable record before resolution. Note that:

   - You can only set fields that currently have the value `nil` (i.e. no
     overriding of already set fields).
   - You can only set fields the record already contains (i.e. records have to
     explicitly contain even optional parameter fields).

   These restrictions are intended to make resolution more predictable. Note
   that you can always use `prepare` directly to perform arbitrary
   injections."
  [params rest-template]
  {:pre [(map? params)]}
  (->ParametersProjection params rest-template))
