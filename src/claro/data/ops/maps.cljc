(ns claro.data.ops.maps
  (:refer-clojure :exclude [select-keys update update-in
                            assoc assoc-in get get-in])
  (:require [claro.data.protocols :as p]
            [claro.data.tree :refer [wrap-tree]]
            [claro.data.ops.chain :as chain]
            [claro.data.ops.fmap :refer [fmap]]
            [clojure.core :as core]))

;; ## Helpers

(defn- assert-map
  [value msg]
  (assert (or (nil? value) (map? value)) (str msg "\n" (pr-str value))))

(defn- wrap-assert-map
  [f msg]
  (fn [value]
    (assert-map value msg)
    (f value)))

;; ## Map Operations

;; ### Selection

(defn select-keys
  "Wrap the given value to select only the given keys once they are available."
  [value ks]
  (chain/chain-eager
    value
    (wrap-assert-map
      #(core/select-keys % ks)
      "can only apply 'select-keys' to resolvables producing maps, given:")))

;; ### Update

(defn update
  "Wrap the given value to perform an update on a key once it's available."
  [value k f & args]
  (let [f #(apply f % args)]
    (chain/chain-eager
      value
      (wrap-assert-map
        (fn [value]
          (core/update value k #(fmap f %)))
        "can only apply 'update' to resolvables producing maps, given:"))))

(defn update-in
  "Wrap the given value to perform an update on a nested key once it's
   available."
  [value [k & rst] f & args]
  (let [f #(apply f % args)]
    (if (empty? rst)
      (update value k f)
      (chain/chain-eager
        value
        (wrap-assert-map
          #(core/update % k update-in rst f)
          "can only apply 'update-in' to resolvables producing maps, given:")))))

;; ### Assoc

(defn assoc
  "Assoc the given value into the given resolvable once it was resolved to a
   map."
  [value k v]
  (chain/chain-eager
    value
    (wrap-assert-map
      #(core/assoc % k v)
      "can only apply 'assoc' to resolvables producing maps, given:")))

(defn assoc-in
  "Assoc the given value into the given resolvable, once the value at
   the given path was resolved to a map."
  [value ks v]
  (if (next ks)
    (let [path (butlast ks)
          k (last ks)]
      (update-in value path #(assoc % k v)))
    (assoc value (first ks) v)))

;; ## Get

(defn get
  ([value k] (get value k nil))
  ([value k default]
   (chain/chain-eager
     value
     (wrap-assert-map
       #(core/get % k default)
       "can only apply 'get' to resolvables producing maps, given:"))))

(defn get-in
  ([value ks] (get-in value ks nil))
  ([value ks default]
   {:pre [(seq ks)]}
   (let [k (first ks)]
     (if-let [ks' (next ks)]
       (chain/chain-eager
         value
         (wrap-assert-map
           #(get-in (get % k) ks' default)
           "can only apply 'get-in' to resolvables producing maps, given:"))
       (get value k default)))))
