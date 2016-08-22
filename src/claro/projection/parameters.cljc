(ns claro.projection.parameters
  (:require [claro.data.protocols :as p]
            [claro.projection.transform :refer [prepare]]))

(defn- assert-resolvable!
  [value]
  (when-not (p/resolvable? value)
    (throw
      (IllegalArgumentException.
        (str
          "'parameters' projection requires a resolvable, given: "
          (pr-str value)))))
  value)

(defn- assert-allowed-params!
  [value params]
  (doseq [[k v] params
          :let [current (get value k ::none)]]
    (when (= current ::none)
      (throw
        (IllegalArgumentException.
          (format
            (str "'parameters' projection requires key '%s' to exist "
                 "in resolvable: %s")
            k
            (pr-str value)))))
    (when-not (nil? current)
      (throw
        (IllegalArgumentException.
          (format
            (str "'parameters' projection cannot override non-nil value "
                 "at '%s' in resolvable: %s")
            k
            (pr-str value))))))
  value)

(defn- inject-params
  [value params]
  (-> value
      (assert-resolvable!)
      (assert-allowed-params! params)
      (into params)))

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
  (prepare #(inject-params % params) rest-template))
