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
        #(core/update % k chain/rechain-eager f)
        "can only apply 'update' to resolvables producing maps, given:"))))

(defn update-in
  [value [k & rst] f & args]
  (let [f #(apply f % args)]
    (if (empty? rst)
      (update value k f)
      (chain/chain-eager
        value
        (wrap-assert-map
          #(core/update % k update-in rst f)
          "can only apply 'update-in' to resolvables producing maps, given:")))))

(defn update-keys
  "Wrap the given value with per-key processing functions (given as a map), where
   each one gets called once the key is available in the given value."
  [value fs]
  (chain/chain-eager
    value
    #(reduce
       (fn [v [k f]]
         (core/update v k chain/rechain-eager f))
       % fs)))
