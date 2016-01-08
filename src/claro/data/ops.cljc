(ns claro.data.ops
  (:refer-clojure :exclude [select-keys update update-in])
  (:require [clojure.core :as core]
            [claro.data.chain :as chain]
            [claro.data.tree :as tree]
            [claro.data.protocols :as p]))

;; ## Helpers

(defn- assert-map
  [value msg]
  (assert (map? value) (str msg "\n" (pr-str value))))

(defn- wrap-assert-map
  [f msg]
  (fn [value]
    (assert-map value msg)
    (f value)))

;; ## Map Operations

(defn- chain-map
  [k value predicate transform re-chain]
  (chain/chain-when
    value
    (wrap-assert-map
      predicate
      (format "can only run '%s' on map, given:" (name k)))
    #(re-chain (transform %))))

(defn select-keys
  "Wrap the given value to select only the given keys once they are available."
  [value ks]
  (if (empty? ks)
    {}
    (or (when (p/resolved? value)
          (core/select-keys value ks))
        (when-not (p/wrapped? value)
          (let [v (p/unwrap-tree1 value)]
            (when (every? #(contains? v %) ks)
              (tree/wrap-tree (core/select-keys v ks)))))
        (chain-map
          :select-keys
          value
          #(some (complement (set ks)) (keys %))
          #(core/select-keys % ks)
          #(select-keys % ks)))))

(defn update
  "Wrap the given value to perform an update on a key once it's available."
  [value k f & args]
  (chain/chain-when
    value
    (wrap-assert-map
      #(contains? % k)
      "can only run 'update' on map, given:")
    #(core/update % k (fn [v] (apply f v args)))))

(defn update-in
  [value [k & rst] f & args]
  (if (empty? rst)
    (update value k #(apply f % args))
    (chain/chain-when
      value
      (wrap-assert-map
        #(contains? % k)
        "can only run 'update-in' on map, given:")
      #(core/update % k update-in rst (fn [v] (apply f v args))))))

(defn- perform-update
  [value fs]
  (let [ks (keys fs)
        found-ks (filter #(contains? value %) ks)]
    (reduce
      (fn [value k]
        (core/update value k (get fs k)))
      value found-ks)))

(defn update-keys
  "Wrap the given value with per-key processing functions (given as a map), where
   each one gets called once the key is available in the given value."
  [value fs]
  (if (empty? fs)
    value
    (chain-map
      :update-keys
      value
      #(some (fn [k] (contains? % k)) (keys fs))
      #(perform-update % fs)
      #(update-keys % (apply dissoc fs (keys %))))))

;; ## Generic Operations

(defn then
  "Wrap the given value with a processing function that gets called the moment
   the given value is neither a `Resolvable` nor wrapped."
  [value f & args]
  (chain/chain-eager value #(apply f % args)))

(defn then!
  "Wrap the given value with a processing function that gets called once the
   value has been fully resolved.

   Only use this for guaranteed finite expansion!"
  [value f & args]
  (chain/chain-blocking value #(apply f % args)))
