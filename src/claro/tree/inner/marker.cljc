(ns claro.tree.inner.map-walker
  (:require [claro.tree.inner
             [navigators :as nav]
             [tree-walker :refer [tree-walker]]]
            [claro.tree :as tree]))

;; ## Resolvable Marker

(defprotocol IMarker
  (key-unresolved! [this])
  (val-unresolved! [this])
  (keys-unresolved? [this])
  (vals-unresolved? [this]))

(deftype Marker [^:volatile-mutable keys-unresolved?
                 ^:volatile-mutable vals-unresolved?]
  IMarker
  (key-unresolved! [this]
    (set! keys-unresolved? true))
  (keys-unresolved? [this]
    keys-unresolved?)

  (val-unresolved! [this]
    (set! vals-unresolved? true))
  (vals-unresolved? [this]
    vals-unresolved?))

;; ## Walker

(def map-walker
  "A specialised tree walker for maps that will remember whether there were
   resolvables in keys or values and only walk over those paths in subsequent
   passes if necessary."
  (let [key-navigator (specter/comp-paths nav/MAP-KEYS)
        val-navigator (specter/comp-paths nav/MAP-VALS)
        key-walker    (tree-walker key-navigator)
        val-walker    (tree-walker val-navigator)]
    (fn all-walker
      [value walk-fn]
      (let [resolvables      (make-resolvable-collector)
            keys-unresolved? (make-unresolved-marker)
            vals-unresolved? (make-unresolved-marker)
            value' (persistent!
                     (reduce-kv
                       (fn [result k v]
                         (let [k' (walk-fn k)
                               v' (walk-fn v)]
                           (when (tree/unresolved? k')
                             (tree/resolvables-into! k' resolvables)
                             (vreset! keys-unresolved? true))
                           (when (tree/unresolved? v')
                             (tree/resolvables-into! v' resolvables)
                             (vreset! vals-unresolved? true))
                           (assoc! result k' v')))
                       (transient {})
                       value))
            unresolved? (or @keys-unresolved? @vals-unresolved?)]
        (if-let [walker (case [@keys-unresolved? @vals-unresolved?]
                          [false false] nil
                          [true  true]  all-walker
                          [true  false] key-walker
                          [false true]  val-walker)]
          (->InnerNode value' walker resolvables [])
          value')))))
