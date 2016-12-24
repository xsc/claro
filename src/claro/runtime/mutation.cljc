(ns claro.runtime.mutation
  (:require [claro.runtime.state :as state]))

(defn- assert-single-mutation!
  [mutations]
  (when (next mutations)
    (throw
      (IllegalStateException.
        (format
          "only one mutation can be resolved per engine run, given %d: %s"
          (count mutations)
          (pr-str mutations)))))
  mutations)

(defn- assert-no-mutations!
  [mutations]
  (when (seq mutations)
    (throw
      (IllegalStateException.
        (str "can only resolve mutations on the top-level: "
             (pr-str (vec mutations)))))))

(defn select-mutation-batches
  [state resolvables]
  (when-let [mutation? (state/opt state :mutation?)]
    (let [mutations (seq (distinct (filter mutation? resolvables)))]
      (if (state/first-iteration? state)
        [(assert-single-mutation! mutations)]
        (assert-no-mutations! mutations)))))
