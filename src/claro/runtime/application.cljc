(ns claro.runtime.application)

(defn apply-resolved-batches
  [{:keys [apply-fn]} value resolvable->value]
  (apply-fn value resolvable->value))
