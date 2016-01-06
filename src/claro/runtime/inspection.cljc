(ns claro.runtime.inspection)

(defn inspect-resolvables
  "Analyze the given value and collect all (remaining) resolvables."
  [{:keys [inspect-fn]} value]
  {:pre [(fn? inspect-fn)]}
  (inspect-fn value))
