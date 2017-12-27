(ns claro.tree.inner.map-walker
  (:require [claro.tree.inner
             [navigators :as nav]
             [node :refer [->InnerNode]]
             [tree-walker :refer [tree-walker]]]
            [claro.tree.protocols :as tree]
            [com.rpl.specter :as specter]))

;; ## Resolvable Marker

(defprotocol MapMarker
  (key-unresolved! [this])
  (val-unresolved! [this])
  (keys-unresolved? [this])
  (vals-unresolved? [this]))

(deftype Marker [^:volatile-mutable keys-unresolved?
                 ^:volatile-mutable vals-unresolved?]
  MapMarker
  (key-unresolved! [this]
    (set! keys-unresolved? true))
  (keys-unresolved? [this]
    keys-unresolved?)

  (val-unresolved! [this]
    (set! vals-unresolved? true))
  (vals-unresolved? [this]
    vals-unresolved?))

(defn- make-marker
  []
  (Marker. false false))

;; ## Walker

(defn- make-resolvable-collector
  "Create a container for a series of resolvables."
  ^java.util.Collection
  []
  (java.util.LinkedList.))

(defn- mark-key!
  [resolvables marker value]
  (when (tree/unresolved? value)
    (tree/resolvables-into! value resolvables)
    (key-unresolved! marker)))

(defn- mark-val!
  [resolvables marker value]
  (when (tree/unresolved? value)
    (tree/resolvables-into! value resolvables)
    (val-unresolved! marker)))

(defn- walk-map!
  [resolvables marker value walk-fn]
  (persistent!
    (reduce-kv
      (fn [result k v]
        (let [k' (walk-fn k)
              v' (walk-fn v)]
          (mark-key! resolvables marker k')
          (mark-val! resolvables marker v')
          (assoc! result k' v')))
      (transient {})
      value)))

(def map-walker
  "A specialised tree walker for maps that will remember whether there were
   resolvables in keys or values and only walk over those paths in subsequent
   passes if necessary."
  (let [key-navigator (specter/comp-paths nav/MAP-KEYS-UNRESOLVED)
        val-navigator (specter/comp-paths nav/MAP-VALS-UNRESOLVED)
        key-walker    (tree-walker key-navigator)
        val-walker    (tree-walker val-navigator)]
    (fn all-walker
      [value walk-fn]
      (let [resolvables (make-resolvable-collector)
            marker      (make-marker)
            value'      (walk-map! resolvables marker value walk-fn)]
        (if-let [walker (case [(keys-unresolved? marker)
                               (vals-unresolved? marker)]
                          [false false] nil
                          [true  true]  all-walker
                          [true  false] key-walker
                          [false true]  val-walker)]
          (->InnerNode value' walker resolvables [])
          value')))))
