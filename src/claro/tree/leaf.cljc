(ns claro.tree.leaf
  (:require [claro.tree.protocols :as tree]))

(deftype LeafNode [value fs]
  tree/Node
  (resolvables-into! [this sq]
    sq)
  (unresolved? [this]
    true)
  (fpartial* [this fs']
    (LeafNode. value (into fs fs')))
  (fold [this resolvable->result]
    (tree/reduce-fns-and-fold fs value resolvable->result))
  (unwrap [this not-possible]
    (if (empty? fs)
      value
      not-possible)))
