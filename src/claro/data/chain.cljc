(ns claro.data.chain
  (:require [claro.data.protocols :as p]
            [claro.data.tree
             [blocking-composition :refer [->BlockingComposition]]
             [composition :refer [->ResolvableComposition]]]
            [claro.data.tree :refer [wrap-tree]]))

(defn chain-when
  "Apply the given function to the (potentially not fully-resolved) value
   once `predicate` is fulfilled."
  [value predicate f]
  (let [f' (comp wrap-tree f)]
    (if (p/resolved? value)
      (if (or (not predicate) (predicate value))
        (f' value)
        (throw
          (IllegalStateException.
            (format "'predicate' does not hold for fully resolved: %s"
                    (pr-str value)))))
      (->ResolvableComposition (wrap-tree value) predicate f'))))

(defn chain-blocking
  "Apply the given function once `value` is fully resolved."
  [value f]
  (let [f' (comp wrap-tree f)]
    (if (p/resolved? value)
      (f' value)
      (->BlockingComposition (wrap-tree value) f'))))

(defn chain-eager
  "Apply the given function once the value is no longer a `Resolvable` or
   wrapped."
  [value f]
  (chain-when value nil f))
