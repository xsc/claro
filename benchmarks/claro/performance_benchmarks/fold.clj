(ns claro.performance-benchmarks.fold
  (:require [perforate.core :refer [defgoal defcase]]
            [claro.tree :as tree]
            [claro.data :as data]))

(defgoal fold
  "Injection of resolution results.")

;; ## Fixtures

(def elements
  (repeatedly 1000 #(reify data/Resolvable)))

(def resolvable->value
  (zipmap elements (range)))

;; ## Cases

(let [value (tree/wrap
              (zipmap
                (map #(str "key-" %) (range))
                elements))]
  (defcase fold :map
    []
    (tree/fold value resolvable->value)))

(let [value (tree/wrap (vec elements))]
  (defcase fold :vector
    []
    (tree/fold value resolvable->value)))

(let [value (tree/wrap (list* elements))]
  (defcase fold :list
    []
    (tree/fold value resolvable->value)))

(let [value (tree/wrap (set elements))]
  (defcase fold :set
    []
    (tree/fold value resolvable->value)))

(let [value (-> (iterate #(hash-map :value %) (first elements))
                (nth 100)
                (tree/wrap))]
  (defcase fold :deep
    []
    (tree/fold value resolvable->value)))
