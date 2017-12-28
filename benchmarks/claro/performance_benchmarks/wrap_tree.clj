(ns claro.performance-benchmarks.wrap-tree
  (:require [perforate.core :refer [defgoal defcase]]
            [claro.tree :as tree]
            [claro.data :as data]))

(defgoal wrap-tree
  "Tree preprocessing, resolvable collection.")

;; ## Fixtures

(def elements
  (repeatedly 1000 #(reify data/Resolvable)))

;; ## Cases

(let [value (zipmap
              (map #(str "key-" %) (range))
              elements)]
  (defcase wrap-tree :map
    []
    (tree/wrap value)))

(let [value (vec elements)]
  (defcase wrap-tree :vector
    []
    (tree/wrap value)))

(let [value (list* elements)]
  (defcase wrap-tree :list
    []
    (tree/wrap value)))

(let [value (set elements)]
  (defcase wrap-tree :set
    []
    (tree/wrap value)))

(let [value (-> (iterate #(hash-map :value %) (first elements))
                (nth 100))]
  (defcase wrap-tree :deep
    []
    (tree/wrap value)))
