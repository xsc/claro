(ns claro.engine.resolver
  (:require [claro.runtime.impl :as impl]
            [claro.data
             [protocols :as p]
             [tree :refer [wrap-tree]]]))

;; ## Helpers

(defn- result-as-map
  [batch result]
  (if (map? result)
    result
    (zipmap batch result)))

(defn- map-kv
  [f m]
  (persistent!
    (reduce
      (fn [m e]
        (assoc! m (key e) (f (key e) (val e))))
      (transient {})
      m)))

;; ## Resolution

(defn- resolve-them-all!
  [impl adapter env [head :as batch]]
  (cond (p/batched-resolvable? head)
        (adapter impl #(p/resolve-batch! head env batch))

        (next batch)
        (let [deferreds (mapv
                          (fn [item]
                            (adapter impl #(p/resolve! item env)))
                          batch)]
          (impl/zip impl deferreds))

        :else
        (let [deferred (adapter impl #(p/resolve! head env))]
          (impl/chain1 impl deferred vector))))

(defn raw-resolve-fn
  "Generate a resolver function for `claro.runtime/run`, suitable for
   processing `claro.data.protocols/Resolvable` values."
  [impl adapter]
  {:pre [impl (fn? adapter)]}
  (fn [env batch]
    (impl/chain1
      impl
      (resolve-them-all! impl adapter env batch)
      #(result-as-map batch %))))

;; ## Transformation

(defn wrap-transform
  "Generate a function to be called after resolution, postprocessing the
   resolved value."
  [impl resolver]
  (fn [env batch]
    (impl/chain1
      impl
      (resolver env batch)
      #(map-kv p/transform %))))

;; ## Finalisation

(defn wrap-finalize
  "Generate a function to be called directly before the resolution result
   is cached."
  [impl resolver]
  (fn [env batch]
    (impl/chain1
      impl
      (resolver env batch)
      (fn [resolvable->value]
        (map-kv #(wrap-tree %2) resolvable->value)))))

;; ## Compound Resolver Function

(defn build
  "Combine the given functions to generate a resolver function suitable for
   the claro runtime."
  [raw-resolve-fn wrappers]
  (reduce #(%2 %1) raw-resolve-fn wrappers))
