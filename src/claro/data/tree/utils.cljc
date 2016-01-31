(ns claro.data.tree.utils
  (:require [claro.data.protocols :as p]))

(defn- can-resolve?
  [tree resolvable->resolved]
  (and (not (p/resolved? tree))
       ;; it seems the following check is slower than just trying to apply the
       ;; resolution ...
       #_(some resolvable->resolved (p/resolvables tree))))

(defn apply-resolution
  [tree resolvable->resolved]
  (if (can-resolve? tree resolvable->resolved)
    (p/apply-resolved-values tree resolvable->resolved)
    tree))

(def all-resolvables-xf
  "Transducer to collect all resolvables in a seq of `ResolvableTree`values."
  (mapcat #(p/resolvables* %)))

(def all-unwrap-xf
  "Transducer to unwrap a collection of elements."
  (map #(p/partial-value % ::this-should-not-happen)))

(defn merge-resolvables
  ([trees]
   (into [] all-resolvables-xf trees))
  ([tree0 tree1]
   (into (p/resolvables* tree0) (p/resolvables* tree1))))
