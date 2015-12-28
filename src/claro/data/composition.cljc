(ns claro.data.composition
  (:require [claro.data
             [resolvable :as r]
             [tree :as tree]]))

;; ## Record

(deftype ConditionalComposition [tree predicate f]
  tree/WrappedTree

  tree/Tree
  (wrap-tree [this]
    (ConditionalComposition. (tree/wrap-tree tree) predicate f))

  tree/ResolvableTree
  (unwrap-tree1 [this]
    (ConditionalComposition. (tree/unwrap-tree1 tree) predicate f))
  (resolved? [_]
    false)
  (resolvables* [_]
    (tree/resolvables* tree))
  (apply-resolved-values [this resolvable->value]
    (let [tree' (tree/apply-resolved-values tree resolvable->value)]
      (cond (= tree tree') this
            (tree/resolved? tree') (-> tree' f tree/wrap-tree)
            (tree/wrapped? tree') (ConditionalComposition. tree' predicate f)
            :else (let [value (tree/unwrap-tree1 tree')]
                    (if (and predicate (predicate value))
                      (-> value f tree/wrap-tree)
                      (ConditionalComposition. tree' predicate f)))))))

;; ## Helpers

(defn- assert-map
  [value msg]
  (assert (map? value) (str msg "\n" (pr-str value))))

(defn- wrap-assert-map
  [f msg]
  (fn [value]
    (assert-map value msg)
    (f value)))

;; ## Resolvable Composition

(defn chain-when
  "Wrap the given value with a processing function that gets called the
   moment the given predicate is fulfilled."
  [value predicate f]
  (->ConditionalComposition value predicate f))

(defn chain-when-contains
  "Wrap the given value with a processing function that gets called once
   the given keys are contained within."
  [value ks f]
  (chain-when
    value
    (wrap-assert-map
      #(every? (fn [k] (contains? % k)) ks)
      "can only run 'chain-when-contains' on a map, given:")
    f))

(defn- chain-map
  [k value predicate transform re-chain]
  (chain-when
    value
    (wrap-assert-map
      predicate
      (format "can only run '%s' on map, given:" (name k)))
    #(re-chain (transform %))))

(defn- update-keys
  [value fs]
  (let [ks (keys fs)
        found-ks (filter #(contains? value %) ks)]
    (reduce
      (fn [value k]
        (update value k (get fs k)))
      value found-ks)))

(defn chain-keys
  "Wrap the given value with per-key processing functions (given as a map), where
   each one gets called once the key is available in the given value."
  [value fs]
  (if (empty? fs)
    value
    (chain-map
      :chain-keys
      value
      #(some (fn [k] (contains? % k)) (keys fs))
      #(update-keys % fs)
      #(chain-keys % (apply dissoc fs (keys %))))))

(defn chain-select-keys
  "Wrap the given value to select only the given keys once they are available."
  [value ks]
  (cond (empty? ks)
        {}

        (tree/resolved? value)
        (select-keys value ks)

        (and (not (tree/wrapped? value))
             (let [v (tree/unwrap-tree1 value)]
               (every? #(contains? v %) ks)))
        (select-keys value ks)

        :else
        (chain-map
          :chain-select-keys
          value
          #(some (complement (set ks)) (keys %))
          #(select-keys % ks)
          #(chain-select-keys % ks))))

(defn- chain*
  [value predicate fs]
  (->> (reverse fs)
       (apply comp)
       (->ConditionalComposition value predicate)))

(defn chain
  "Wrap the given value with processing functions that get called (in-order)
   the moment the given value is neither a `Resolvable` nor wrapped."
  [value f & fs]
  (chain* value (constantly true) (cons f fs)))

(defn wait
  "Wrap the given value with processing functions that get called (in-order)
   once the value has been fully resolved. Only use this for guaranteed finite
   expansion."
  [value f & fs]
  (chain* value nil (cons f fs)))
