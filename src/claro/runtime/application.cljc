(ns claro.runtime.application
  (:require [claro.runtime.state :as state]))

(defn apply-resolved-batches
  [state resolvable->value]
  (let [apply-fn (state/opt state :apply-fn)
        value    (state/value state)]
    (apply-fn value resolvable->value)))
