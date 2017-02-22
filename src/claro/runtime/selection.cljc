(ns claro.runtime.selection
  (:require [claro.runtime.state :as state]))

(defn- assert-class-selected!
  "Make sure at least a single class was selected, otherwise throw
   an `IllegalStateException`."
  [classes]
  (when (empty? classes)
    (throw
      (IllegalStateException.
        "resolvables were available but 'select-fn' did not choose any.")))
  classes)

(defn- assert-classes-valid!
  "Make sure all selected classes were actually candidates, otherwise
   throw an `IllegalStateException`."
  [resolvables classes]
  (doseq [class classes]
    (when-not (contains? resolvables class)
      (throw
        (IllegalStateException.
          (str "'select-fn' chose an unknown resolvable class:" class)))))
  classes)

(defn select-resolvable-batches
  "Use the given `select-fn` (seq of classes -> seq of classes) and
   `inspect-fn` (value -> seq of resolvables) to collect batches of
   resolvables. Returns a seq of such batches."
  [state resolvables]
  (let [select-fn (state/opt state :select-fn)
        partition-fn (state/opt state :partition-fn vector)
        by-class (group-by class (distinct resolvables))]
    (->> (select-fn by-class)
         (assert-class-selected!)
         (assert-classes-valid! by-class)
         (mapcat (comp partition-fn by-class))
         (vec))))
