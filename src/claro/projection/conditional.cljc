(ns claro.projection.conditional
  (:require [claro.projection.protocols :as pr]
            [claro.data.protocols :as p]
            [claro.data.ops.then :refer [then]]))

;; ## Record

(defn- project-match
  [condition->template else-template value]
  (or (some
        (fn [[condition template]]
          (if (condition value)
            (pr/project-template template value)))
        condition->template)
      (if else-template
        (then value #(pr/project-template else-template %)))
      (if (p/resolvable? value)
        (then value #(project-match condition->template else-template %)))))

(defrecord ConditionalProjection [condition->template else-template]
  pr/Projection
  (project-template [_ value]
    (project-match condition->template else-template value)))

;; ## Constructors

(defn conditional
  "Apply the first projection template whose predicate matches the value.
   `:else` can be given to denote the default case.

   ```
   (projection/conditional
     #(= (:type %) :animal) {:left-paw projection/leaf}
     #(= (:type %) :human)  {:left-hand projection/leaf})
   ```
   "
  [condition template & more]
  {:pre [(even? (count more))]}
  (let [pairs (cons [condition template] (partition 2 more))
        [c maybe-else-template] (last pairs)
        [condition->template else-template]
        (if (= c :else)
          [(butlast pairs) maybe-else-template]
          [pairs nil]) ]
    (->ConditionalProjection condition->template else-template)))
