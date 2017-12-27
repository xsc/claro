(ns claro.tree.resolvable
  (:require [claro.tree.protocols :as tree]))

(deftype ResolvableNode [value fs]
  tree/Node
  (resolvables-into! [this sq]
    (.add ^java.util.Collection sq value))
  (unresolved? [this]
    true)
  (fpartial* [this fs']
    (ResolvableNode. value (into fs fs')))
  (fold [this resolvable->result]
    (let [value' (resolvable->result value ::none)]
      (cond (= value' ::none)
            this

            (tree/unresolved? value')
            (tree/reduce-fns-and-fold fs value' {})

            :else
            (tree/reduce-fns fs value'))))
  (unwrap [this not-possible]
    (if (empty? fs)
      value
      not-possible))

  tree/PartialNode
  (unwrap-partial [this not-possible]
    not-possible))
