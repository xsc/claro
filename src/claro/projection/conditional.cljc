(ns claro.projection.conditional
  (:require [claro.projection.protocols :as pr]
            [claro.data.protocols :as p]
            [claro.data.ops.chain :refer [chain-eager chain-when]]))

;; ## Record

(defn- project-match
  [condition->template else-template value]
  (second
    (or (some
          (fn [[condition template]]
            (if (condition value)
              [:done (pr/project template value)]))
          condition->template)
        (if else-template
          [:done (pr/project else-template value)]))))

(defrecord ConditionalProjection [predicate condition->template else-template]
  pr/Projection
  (project [_ value]
    (let [f #(project-match condition->template else-template %)]
      (if predicate
        (chain-when value predicate f)
        (chain-eager value f)))))

;; ## Constructors

(defn- make-conditional
  [predicate condition template more]
  (let [pairs (cons [condition template] (partition 2 more))
        [c maybe-else-template] (last pairs)
        [condition->template else-template]
        (if (= c :else)
          [(butlast pairs) maybe-else-template]
          [pairs nil]) ]
    (->ConditionalProjection predicate condition->template else-template)))

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
  (make-conditional nil condition template more))

(defn conditional-when
  "Like `conditional` but the template will only be applied once the given
   `predicate` holds."
  [predicate condition template & more]
  {:pre [(even? (count more))]}
  (make-conditional predicate condition template more))
