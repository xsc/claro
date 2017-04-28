(ns claro.data.tree.collection
  (:require [claro.data.protocols :as p]
            [claro.data.tree.tuple :as tuple])
  (:import [claro.data.protocols ResolvableTree]))

;; ## Tree Type

(deftype ResolvableCollection [prototype tuple]
  ResolvableTree
  (wrapped? [_]
    false)
  (processable? [_]
    false)
  (resolved? [_]
    false)
  (unwrap-tree [tree]
    tree)
  (partial-value [_ _]
    (prototype (p/partial-value tuple ::none)))
  (resolvables* [_]
    (p/resolvables* tuple))
  (apply-resolved-values [tree resolvable->values]
    (let [tuple' (p/apply-resolved-values tuple resolvable->values)]
      (if (p/resolved? tuple')
        (prototype tuple')
        (ResolvableCollection. prototype tuple')))))

(defmethod print-method ResolvableCollection
  [^ResolvableCollection value ^java.io.Writer writer]
  (print-method (p/partial-value value ::none) writer))

;; ## Constructor

(defn make
  [wrap-fn prototype elements]
  (let [tuple (tuple/make wrap-fn elements)]
    (if (p/resolved? tuple)
      (prototype tuple)
      (ResolvableCollection. prototype tuple))))
