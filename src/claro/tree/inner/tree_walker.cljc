(ns claro.tree.inner.tree-walker
  (:require [claro.tree.inner.node :refer [->InnerNode]]
            [claro.tree.protocols :as tree]
            [com.rpl.specter :as specter]))

(defn- make-resolvable-collector
  "Create a container for a series of resolvables."
  ^java.util.Collection
  []
  (java.util.LinkedList.))

(defn- make-unresolved-marker
  []
  (volatile! false))

(defn- walk-tree-and-collect
  "Use the given navigator to apply `walk-fn` to every selected value,
   collecting any resolvables into `resolvables`."
  [navigator resolvables marker value walk-fn]
  (specter/compiled-transform
    navigator
    (fn [value]
      (let [result (walk-fn value)]
        (when (tree/unresolved? result)
          (tree/resolvables-into! result resolvables)
          (vreset! marker true))
        result))
    value))

(defn tree-walker
  "Create a generic tree walker that will transform all values identified
   by the given specter path."
  [navigator]
  (fn self
    [value walk-fn]
    (let [resolvables (make-resolvable-collector)
          marker      (make-unresolved-marker)
          value'      (walk-tree-and-collect
                        navigator
                        resolvables
                        marker
                        value
                        walk-fn)]
      (if @marker
        (->InnerNode value' self resolvables [])
        value'))))
