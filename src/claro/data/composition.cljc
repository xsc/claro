(ns claro.data.composition
  (:require [claro.data
             [resolvable :as r]
             [tree :as tree]]))

;; ## Record

(defn- apply-composition?
  [predicate value]
  (and (not (r/resolvable? value))
       (not (tree/wrapped? value))
       (or (tree/resolved? value)
           (when predicate
             (predicate value)))))

(defrecord ConditionalResolvable [tree-resolvable predicate f]
  tree/TreeResolvable
  (resolvable [_]
    (tree/resolvable tree-resolvable))
  (set-resolved-value [this resolved-value]
    (assoc this
           :tree-resolvable
           (tree/set-resolved-value tree-resolvable resolved-value)))
  (resolve-in [this {:keys [value] :as conditional}]
    (let [{:keys [value resolvables]} (tree/resolve-in tree-resolvable value)]
      (if (apply-composition? predicate value)
        (let [value' (f value)]
          {:value       value'
           :resolvables (tree/tree-resolvables value')})
        {:value       (assoc conditional :value value)
         :resolvables (map #(assoc this :tree-resolvable %) resolvables)}))))

(defrecord ConditionalComposition [value predicate f]
  tree/WrappedTree
  (wrapped? [_] true)

  tree/ResolvableTree
  (tree-resolvables [_]
    (map
      #(ConditionalResolvable. % predicate f)
      (tree/tree-resolvables value)))
  (resolved? [_]
    false))

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
  (if (apply-composition? predicate value)
    (f value)
    (->ConditionalComposition value predicate f)))

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
  (if (tree/resolved? value)
    (transform value)
    (chain-when
      value
      (wrap-assert-map
        predicate
        (format "can only run '%s' on map, given:" (name k)))
      #(re-chain (transform %)))))

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

        (apply-composition?
          (fn [v] (every? #(contains? v %) ks))
          value)
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
