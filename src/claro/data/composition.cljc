(ns claro.data.composition
  (:require [claro.data
             [resolvable :as r]
             [resolvable-wrapper :as w]]))

;; ## Record

(defn- apply-composition?
  [predicate value]
  (and (not (r/resolvable? value))
       (not (w/wrapped? value))
       (or (w/resolved? value)
           (when predicate
             (predicate value)))))

(defrecord ConditionalComposition [value predicate f]
  w/WrappedResolvable
  w/ResolvableWrapper
  (resolvables [_]
    (w/resolvables value))
  (apply-resolved [_ resolved-values]
    (let [value' (w/apply-resolved value resolved-values)]
      (if (apply-composition? predicate value')
        (f value')
        (->ConditionalComposition value' predicate f)))))

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
  (if (w/resolved? value)
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
  (if (empty? ks)
    {}
    (chain-map
      :chain-select-keys
      value
      #(some (complement (set ks)) (keys %))
      #(select-keys % ks)
      #(chain-select-keys % ks))))

(defn chain
  "Wrap the given value with a processing function that gets called once the
   value has been fully resolved. Only use this for guaranteed finite expansion."
  [value f & fs]
  (->> (cons f fs)
       (reverse)
       (apply comp)
       (->ConditionalComposition value nil)))
