(ns claro.runtime.selection)

(defn- assert-class-selected!
  "Make sure at least a single class was selected, otherwise throw
   an `IllegalStateException`."
  [classes]
  (when (empty? classes)
    (throw
      (IllegalStateException.
        "resolvables were available but 'selector' did not choose any.")))
  classes)

(defn- assert-classes-valid!
  "Make sure all selected classes were actually candidates, otherwise
   throw an `IllegalStateException`."
  [resolvables classes]
  (doseq [class classes]
    (when-not (contains? resolvables class)
      (throw
        (IllegalStateException.
          (str "'selector' chose an unknown resolvable class:" class)))))
  classes)

(defn select-resolvable-batches
  "Use the given `selector` (seq of classes -> seq of classes) and
   `inspect-fn` (value -> seq of resolvables) to collect batches of
   resolvables. Returns a seq of such batches."
  [{:keys [selector] :or {selector identity}} resolvables]
  (let [by-class (group-by class resolvables)]
    (some->> (seq (keys by-class))
             (selector)
             (assert-class-selected!)
             (assert-classes-valid! by-class)
             (mapv #(get by-class %)))))
