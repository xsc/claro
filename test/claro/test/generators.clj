(ns claro.test.generators
  (:require [clojure.test.check.generators :as gen]
            [clojure.set :as set]
            [claro.data.protocols :as p]
            [claro.tree :as tree]))

;; ## Types

(defrecord R [x]
  p/Resolvable)

(defrecord BR [x]
  p/BatchedResolvable)

(defrecord WrapperRecord [inner])

;; ## Value Generators

(let [NaN? #(and (number? %) (Double/isNaN %))]
  (def any
    "Generator for any value, excluding generation of NaN, b/c NaN breaks equality."
    (gen/recursive-gen
      gen/container-type
      (gen/such-that
        (complement NaN?)
        gen/simple-type))))

(def error
  "Generator for error containers."
  (gen/let [message gen/string-ascii
            data    (gen/one-of
                      [(gen/hash-map :context gen/string-ascii)
                       (gen/return nil)])]
    (gen/return
      (tree/error message data))))

(def resolvable
  (gen/one-of
    [(gen/fmap ->R gen/s-pos-int)
     (gen/fmap ->BR gen/s-pos-int)]))

;; ## Tree Generators

(defn- tree-plain-value
  [g]
  (gen/fmap
    #(hash-map
       :value       %
       :node        %
       :resolvables #{})
    g))

(def ^:private tree-resolvable
  (->> resolvable
       (gen/fmap
         #(hash-map
            :value       (:x %)
            :node        %
            :resolvables (hash-set %)))))

(def ^:private tree-leaf
  (gen/frequency
    [[4  tree-resolvable]
     [1  (tree-plain-value error)]
     [10 (tree-plain-value gen/string-ascii)]]))

(defn- tree-collection-node
  [g]
  (->> (gen/one-of
         [(gen/list g)
          (gen/set g)
          (gen/vector g)])
       (gen/fmap
         (fn [sq]
           (reduce
             (fn [acc {:keys [value node resolvables]}]
               (-> acc
                   (update :value conj value)
                   (update :node conj node)
                   (update :resolvables set/union resolvables)))
             {:value (empty sq)
              :node  (empty sq)
              :resolvables #{}}
             sq)))))

(defn- tree-map-node
  [g]
  (gen/let [s gen/pos-int
            f (gen/elements [identity ->WrapperRecord])]
    (gen/let [ks (gen/vector-distinct-by :value g {:num-elements s})
              vs (gen/vector g s)]
      (let [value (zipmap (map :value ks) (map :value vs))
            node  (zipmap (map :node ks) (map :node vs))
            resolvables (->> (concat
                               (map :resolvables ks)
                               (map :resolvables vs))
                             (reduce set/union))]
        {:value       (f value)
         :node        (f node)
         :resolvables resolvables}))))

(def tree
  (->> (gen/recursive-gen
         (fn [g]
           (gen/frequency
             [[5  (tree-collection-node g)]
              [10 (tree-map-node g)]]))
         tree-leaf)
       (gen/fmap
         (fn [{:keys [resolvables] :as tree}]
           (->> resolvables
                (map (juxt identity :x))
                (into {})
                (assoc tree :resolvable->result))))))
