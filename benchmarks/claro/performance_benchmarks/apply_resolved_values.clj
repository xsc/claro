(ns claro.performance-benchmarks.apply-resolved-values
  (:require [perforate.core :refer [defgoal defcase]]
            [claro.data.protocols :as p]
            [claro.data.tree :as tree]
            [claro.data :as data]))

(defgoal apply-resolved-values
  "Injection of resolution results.")

;; ## Fixtures

(def elements
  (repeatedly 1000 #(reify data/Resolvable)))

(def resolvable->value
  (zipmap elements (range)))

;; ## Cases

(let [value (tree/wrap-tree
              (zipmap
                (map #(str "key-" %) (range))
                elements))]
  (defcase apply-resolved-values :map
    []
    (p/apply-resolved-values value resolvable->value)))

(let [value (tree/wrap-tree (vec elements))]
  (defcase apply-resolved-values :vector
    []
    (p/apply-resolved-values value resolvable->value)))

(let [value (tree/wrap-tree (list* elements))]
  (defcase apply-resolved-values :list
    []
    (p/apply-resolved-values value resolvable->value)))

(let [value (tree/wrap-tree (set elements))]
  (defcase apply-resolved-values :set
    []
    (p/apply-resolved-values value resolvable->value)))

(let [value (-> (iterate #(hash-map :value %) (first elements))
                (nth 100)
                (tree/wrap-tree))]
  (defcase apply-resolved-values :deep
    []
    (p/apply-resolved-values value resolvable->value)))
