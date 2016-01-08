(ns claro.data.ops.maps
  (:refer-clojure :exclude [select-keys update update-in])
  (:require [claro.data.protocols :as p]
            [claro.data.tree :refer [wrap-tree]]
            [claro.data.ops.chain :as chain]
            [clojure.core :as core]))

;; ## Helpers

(defn- assert-map
  [value msg]
  (assert (map? value) (str msg "\n" (pr-str value))))

(defn- wrap-assert-map
  [f msg]
  (fn [value]
    (assert-map value msg)
    (f value)))

(defn- chain-map
  [k value predicate transform re-chain]
  (chain/chain-when
    value
    (wrap-assert-map
      predicate
      (format "can only run '%s' on map, given:" (name k)))
    #(re-chain (transform %))))

;; ## Map Operations

;; ### Selection

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
              (wrap-tree (core/select-keys v ks)))))
        (chain-map
          :select-keys
          value
          #(some (complement (set ks)) (keys %))
          #(core/select-keys % ks)
          #(select-keys % ks)))))

;; ### Update

(defn update
  "Wrap the given value to perform an update on a key once it's available."
  [value k f & args]
  (chain/chain-eager
    value
    (wrap-assert-map
      #(core/update % k (fn [v] (apply f v args)))
      "can only apply 'update' to resolvables producing maps, given:")))

(defn update-in
  [value [k & rst] f & args]
  (if (empty? rst)
    (update value k #(apply f % args))
    (chain/chain-eager
      value
      (wrap-assert-map
        #(core/update % k update-in rst (fn [v] (apply f v args)))
        "can only apply 'update-in' to resolvables producing maps, given:"))))

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
