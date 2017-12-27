(ns claro.tree.blocking
  (:require [claro.tree.protocols :as tree]))

(deftype BlockingNode [value fs]
  tree/Node
  (resolvables-into! [this sq]
    (tree/resolvables-into! value sq))
  (unresolved? [this]
    true)
  (fpartial* [this fs]
    (tree/fmap* this fs))
  (fold [this resolvable->result]
    (let [value' (tree/fold value resolvable->result)]
      (if (tree/unresolved? value')
        (BlockingNode. value' fs)
        (tree/reduce-fns-and-fold
          tree/fmap*
          fs
          value'
          resolvable->result))))
  (unwrap [this not-possible]
    not-possible)

  tree/BlockingNode
  (fmap* [this fs']
    (BlockingNode. value (into fs fs'))))
