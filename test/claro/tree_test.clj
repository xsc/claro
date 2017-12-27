(ns claro.tree-test
  (:require [clojure.test.check :as tc]
            [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [clojure.test :refer :all]
            [claro.test :as test]
            [claro.test.generators :as tgen]
            [clojure.set :as set]
            [claro.data.protocols :refer [Resolvable resolvable?]]
            [claro.tree :as tree]))

;; ## Helper

(defn- make-observer
  [& trees]
  (let [observed           (atom #{})
        resolvable->result (into {} (map :resolvable->result) trees)]
    (-> (fn [resolvable not-found]
          (swap! observed conj resolvable)
          (resolvable->result resolvable not-found))
        (with-meta
          {:observed observed}))))

(defn- merge-resolvables
  [& trees]
  (reduce into #{} (map :resolvables trees)))

(defn- read-observed
  [observer]
  (-> observer meta :observed deref))

(defn- has-observed?
  [observer & trees]
  (= (read-observed observer) (apply merge-resolvables trees)))

(defn- has-observed-none?
  [observer & trees]
  (empty? (read-observed observer)))

;; ## Tests

;; ### Base Cases

(defspec t-plain-values (test/times 100)
  (prop/for-all
    [value tgen/any]
    (let [tree (tree/wrap value)]
      (and (tree/resolved? tree)
           (= tree value)))))

(defspec t-unresolved? (test/times 100)
  (prop/for-all
    [{:keys [node resolvables]} tgen/tree]
    (let [tree (tree/wrap node)]
      (or (tree/unresolved? tree)
          (empty? resolvables)))))

(defspec t-resolvables (test/times 100)
  (prop/for-all
    [{:keys [node resolvables]} tgen/tree]
    (let [tree (tree/wrap node)]
      (= resolvables (tree/resolvables tree)))))

(defspec t-fold (test/times 100)
  (prop/for-all
    [{:keys [value node resolvables resolvable->result]} tgen/tree]
    (let [tree               (tree/wrap node)
          result             (tree/fold tree resolvable->result)]
      (and (tree/resolved? result)
           (= result value)))))

(defspec t-fold-partial (test/times 100)
  (prop/for-all
    [{:keys [value node resolvables resolvable->result]} tgen/tree
     to-resolve-count                 gen/pos-int]
    (let [tree               (tree/wrap node)
          to-resolve         (set (take to-resolve-count resolvables))
          resolvable->result (select-keys resolvable->result to-resolve)
          result             (tree/fold tree resolvable->result)
          remaining          (tree/resolvables result)]
      (= remaining (set/difference resolvables to-resolve)))))

(defspec t-fold-can-inject-a-composed-value (test/times 100)
  (prop/for-all
    [resolvable tgen/resolvable
     delta      gen/int]
    (let [value              (:x resolvable)
          node               (tree/wrap resolvable)
          resolvable->result {resolvable (tree/fpartial
                                           #(+ % delta)
                                           value)}
          result             (tree/fold node resolvable->result)]
      (= (+ value delta) result))))

(defspec t-fold-does-not-run-into-an-infinite-loop (test/times 100)
  (prop/for-all
    [first-resolvable tgen/resolvable
     second-resolvable tgen/resolvable]
    (let [first-node         (tree/wrap first-resolvable)
          second-node        (tree/wrap second-resolvable)
          resolvable->result (assoc {}
                                    first-resolvable  second-node
                                    second-resolvable first-node)
          result             (tree/fold first-node resolvable->result)]
      (tree/unresolved? result))))

;; ### `fpartial`

(defspec t-fpartial (test/times 100)
  (prop/for-all
    [{:keys [value node resolvables]} tgen/tree]
    (let [observer (make-observer)
          tree     (tree/fpartial :unknown {:node node})
          result   (tree/fold tree observer)]
      (and (tree/unresolved? tree)
           (nil? result)
           (has-observed-none? observer)))))

(defspec t-fpartial-multiple-arguments (test/times 100)
  (prop/for-all
    [first-tree  tgen/tree
     second-tree tgen/tree]
    (let [index-node (tgen/->R 0)
          observer   (make-observer first-tree)
          node       [(:node first-tree) (:node second-tree)]
          tree       (tree/fpartial nth node index-node)
          result     (-> tree
                         (tree/fold {index-node 0})
                         (tree/fold observer))]
      (and (tree/unresolved? tree)
           (= (:value first-tree) result)
           (has-observed? observer first-tree)))))

(defspec t-fpartial-resolved-arguments (test/times 100)
  (prop/for-all
    [first-tree  tgen/tree
     second-tree tgen/tree]
    (let [index-node (tgen/->R 0)
          observer   (make-observer first-tree)
          node       [(:node first-tree) (:node second-tree)]
          tree       (tree/fpartial nth node 0)
          result     (tree/fold tree observer)]
      (and (tree/unresolved? tree)
           (= (:value first-tree) result)
           (has-observed? observer first-tree)))))

;; ### `fmap`

(defspec t-fmap (test/times 100)
  (prop/for-all
    [{:keys [value node resolvables] :as spec} tgen/tree]
    (let [observer (make-observer spec)
          tree     (tree/fmap :unknown {:node node})
          result   (tree/fold tree observer)]
      (and (tree/unresolved? tree)
           (nil? result)
           (has-observed? observer spec)))))

(defspec t-fmap-multiple-arguments (test/times 100)
  (prop/for-all
    [first-tree  tgen/tree
     second-tree tgen/tree]
    (let [index-node  (tgen/->R 0)
          observer    (make-observer first-tree second-tree)
          node        [(:node first-tree) (:node second-tree)]
          tree        (tree/fmap nth node index-node)
          result      (-> tree
                          (tree/fold {index-node 0})
                          (tree/fold observer))]
      (and (tree/unresolved? tree)
           (= (:value first-tree) result)
           (has-observed? observer first-tree second-tree)))))

;; ### `ftransform`

(defspec t-ftransform (test/times 100)
  (prop/for-all
    [original    tgen/tree
     replacement tgen/tree]
    (let [observer (make-observer original replacement)
          tree     (->> (:node original)
                        (tree/ftransform
                          (constantly (:node replacement))))
          result   (tree/fold tree observer)]
      (and (tree/unresolved? tree)
           (= (:value replacement) result)
           (has-observed? observer replacement)))))

(defspec t-ftransform-on-composition (test/times 100)
  (prop/for-all
    [original    tgen/tree
     replacement tgen/tree
     comp-fn     (gen/elements [tree/fpartial tree/fmap])]
    (let [observer (make-observer original replacement)
          tree     (->> (:node original)
                        (comp-fn vector)
                        (tree/ftransform
                          (constantly (:node replacement))))
          result   (tree/fold tree observer)]
      (and (tree/unresolved? tree)
           (= (:value replacement) result)
           (has-observed? observer original replacement)))))

(defspec t-ftransform-called-multiple-times (test/times 100)
  (prop/for-all
    [original    tgen/tree
     replacement tgen/tree]
    (let [observer (make-observer original replacement)
          tree     (->> (:node original)
                        (tree/ftransform vector)
                        (tree/ftransform
                          (constantly (:node replacement))))
          result   (tree/fold tree observer)]
      (and (tree/unresolved? tree)
           (= (:value replacement) result)
           (has-observed? observer replacement)))))

;; ### Error Values

(def ^:private gen-composition
  (gen/elements
    [tree/fmap
     tree/fpartial
     tree/ftransform]))

(defspec t-composition-can-produce-error (test/times 100)
  (prop/for-all
    [{:keys [node resolvable->result]} tgen/tree
     error                             tgen/error
     before                            gen-composition
     after                             gen-composition]
    (let [tree (->> node
                    (before (constantly error))
                    (after str))]
      (is (= error (tree/fold tree resolvable->result))))))

(defspec t-composition-maintains-single-error (test/times 100)
  (prop/for-all
    [error   tgen/error
     comp-fn gen-composition]
    (= error
       (-> (comp-fn str error)
           (tree/fold {})))))

(defspec t-composition-maintains-first-error (test/times 100)
  (prop/for-all
    [{:keys [node resolvable->result]} tgen/tree
     first-error                       tgen/error
     second-error                      tgen/error
     comp-fn                           (gen/elements [tree/fpartial tree/fmap])]
    (let [tree (comp-fn str node first-error second-error)]
      (= first-error (tree/fold tree resolvable->result)))))
