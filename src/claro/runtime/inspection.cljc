(ns claro.runtime.inspection
  (:require [claro.runtime.state :as state]))

(defn inspect-resolvables
  "Analyze the given value and collect all (remaining) resolvables."
  [state]
  (let [inspect-fn (state/opt state :inspect-fn)]
    (inspect-fn (state/value state))))
