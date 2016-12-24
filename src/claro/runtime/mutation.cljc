(ns claro.runtime.mutation)

(defn- assert-single-mutation!
  [mutations]
  (when (next mutations)
    (throw
      (IllegalStateException.
        (format
          "only one mutation can be resolved per engine run, given %d: %s"
          (count mutations)
          (pr-str mutations))))))

(defn- assert-no-mutations!
  [{:keys [mutation?]} {:keys [batch-count]} resolvables]
  (when (and (pos? batch-count) mutation?)
    (when-let [candidate (some #(when (mutation? %) %) resolvables)]
      (throw
        (IllegalStateException.
          (str "can only resolve mutations on the top-level: "
               (pr-str candidate)))))))

(defn select-mutation-batches
  [{:keys [mutation?] :as opts}
   {:keys [batch-count cache] :as state}
   resolvables]
  (when mutation?
    (if (zero? batch-count)
      (when-let [mutations (seq (distinct (filter mutation? resolvables)))]
        (assert-single-mutation! mutations)
        [mutations])
      (assert-no-mutations! opts state resolvables))))
