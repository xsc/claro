(ns claro.tree.inner.node
  (:require [claro.tree.protocols :as tree]))

(deftype InnerNode [value walker resolvables fs]
  tree/Node
  (resolvables-into! [this sq]
    (.addAll ^java.util.Collection sq resolvables))
  (unresolved? [this]
    true)
  (fpartial* [this fs']
    (InnerNode. value walker resolvables (into fs fs')))
  (fold [this resolvable->result]
    (if (empty? fs)
      (walker value #(tree/fold % resolvable->result))
      (tree/reduce-fns-and-fold fs value resolvable->result)))
  (unwrap [this not-possible]
    (if (empty? fs)
      value
      not-possible)))
