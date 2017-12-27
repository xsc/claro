(ns claro.data.error
  (:require [claro.data.protocols :as p]))

;; ## Record

(deftype ResolutionError [message data])

(defmethod print-method ResolutionError
  [^ResolutionError value ^java.io.Writer writer]
  (.write writer "#<claro/error ")
  (print-method (.-message value) writer)
  (when-let [data (.-data value)]
    (.write writer ", ")
    (print-method data writer))
  (.write writer ">"))

;; ## Constructor

(defn ^{:added "0.2.1"} error
  "Generate a value representing a resolution error."
  [message & [data]]
  {:pre [(string? message)
         (or (nil? data) (map? data))]}
  (->ResolutionError message data))

(defn ^{:added "0.2.1"} error?
  "Check whether the given value represents a resolution error."
  [value]
  (instance? ResolutionError value))

(defn ^{:added "0.2.1"} error-message
  "Retrieve the message from the given [[error]] value."
  [^ResolutionError e]
  (.-message e))

(defn ^{:added "0.2.1"} error-data
  "Retrieve error data from the given [[error]] value."
  [^ResolutionError e]
  (.-data e))

(defn ^{:added "0.2.1"} collect-errors
  "Find all errors within the given value."
  [value]
  (->> (tree-seq coll? seq value)
       (filter error?)))

(defmacro with-error?
  "Helper macro that short-circuits if `value` is an [[error]]."
  [value & body]
  `(let [v# ~value]
     (if (error? v#)
       v#
       (do ~@body))))

(defmacro ^{:added "0.2.9"} unless-error->
  [value & body]
  (if-let [[f & rst] (seq body)]
    `(let [v# ~value]
       (if (error? v#)
         v#
         (unless-error-> (-> v# ~f) ~@rst)))
    value))

(defmacro ^{:added "0.2.9"} unless-error->>
  [value & body]
  (if-let [[f & rst] (seq body)]
    `(let [v# ~value] (if (error? v#)
         v#
         (unless-error->> (->> v# ~f) ~@rst)))
    value))
