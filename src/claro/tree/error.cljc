(ns claro.tree.error
  (:require [claro.tree.protocols :as tree]))

;; ## Node
;;
;; Error nodes should not be affected by any operations.

(deftype ErrorNode [message data]
  tree/Node
  (resolvables-into! [this sq]
    sq)
  (unresolved? [this]
    false)
  (fpartial* [this fs']
    this)
  (fold [this resolvable->result]
    this)
  (unwrap [this not-possible]
    not-possible)

  tree/ErrorNode
  (error? [this]
    true)
  (error-message [this]
    message)
  (error-data [this]
    data)

  tree/TransformableNode
  (ftransform* [this f]
    this)

  tree/BlockingNode
  (fmap* [this fs]
    this))

;; ## Functions

(defn ^{:added "0.3.0"} error
  "Generate a value representing a resolution error."
  [message & [data]]
  {:pre [(string? message)
         (or (nil? data) (map? data))]}
  (ErrorNode. message data))

(defn ^{:added "0.3.0"} collect-errors
  "Find all errors within the given value."
  [value]
  (->> (tree-seq coll? seq value)
       (filter tree/error?)))

(defmacro with-error?
  "Helper macro that short-circuits if `value` is an [[error]]."
  [value & body]
  `(let [v# ~value]
     (if (tree/error? v#)
       v#
       (do ~@body))))

(defmacro ^{:added "0.2.9"} unless-error->
  [value & body]
  (if-let [[f & rst] (seq body)]
    `(let [v# ~value]
       (if (tree/error? v#)
         v#
         (unless-error-> (-> v# ~f) ~@rst)))
    value))

(defmacro ^{:added "0.2.9"} unless-error->>
  [value & body]
  (if-let [[f & rst] (seq body)]
    `(let [v# ~value]
       (if (tree/error? v#)
         v#
         (unless-error->> (->> v# ~f) ~@rst)))
    value))

(defn ^{:added "0.3.0"} error-aware
  [f]
  (fn [& args]
    (or (some
          #(when (tree/error? %)
             %)
          args)
        (apply f args))))
