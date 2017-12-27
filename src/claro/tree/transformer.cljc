(ns claro.tree.transformer
  (:require [claro.tree.protocols :as tree]))

;; ## Node for immediate Transformations

(deftype TransformedNode [value-delay fs]
  tree/Node
  (resolvables-into! [this sq]
    (tree/resolvables-into! @value-delay sq))
  (unresolved? [this]
    true)
  (fpartial* [this fs']
    (TransformedNode. value-delay (into fs fs')))
  (fold [this resolvable->result]
    (tree/reduce-fns-and-fold fs @value-delay resolvable->result))
  (unwrap [this not-possible]
    (if (empty? fs)
      (tree/unwrap @value-delay not-possible)
      not-possible)))

;; ## Node for delayed Transformations

(deftype TransformerNode* [f value fs]
  tree/Node
  (resolvables-into! [this sq]
    (tree/resolvables-into! value sq))
  (unresolved? [this]
    true)
  (fpartial* [this fs']
    (TransformerNode*. f value (into fs fs')))
  (fold [this resolvable->result]
    (let [value (tree/fold value resolvable->result)
          value' (tree/unwrap value ::none)]
      (if (= value' ::none)
        (-> (tree/ftransform* value f)
            (tree/fpartial* fs))
        (tree/reduce-fns-and-fold fs (f value') resolvable->result)))))

;; ## Constructor

(defn ->TransformerNode
  [f value]
  (let [value' (tree/unwrap value ::none)]
    (if (= value' ::none)
      (TransformerNode*. f value [])
      (TransformedNode. (delay (f value)) []))))
