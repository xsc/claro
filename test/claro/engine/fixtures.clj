(ns claro.engine.fixtures
  (:require [claro.engine :as engine]))

(defn make-engine
  ([] (make-engine (atom []) nil))
  ([v] (if (map? v)
         (make-engine (atom []) v)
         (make-engine v nil)))
  ([resolutions more-opts]
   (engine/wrap
     (engine/engine (merge {:max-cost Long/MAX_VALUE} more-opts))
     (fn [f]
       (fn [env batch]
         (swap! resolutions
                (fnil conj [])
                [(class (first batch)) (count batch)])
         (f env batch))))))
