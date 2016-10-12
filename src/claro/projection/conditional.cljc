(ns claro.projection.conditional
  (:require [claro.projection.protocols :as pr]
            [claro.projection.union :refer [union*]]
            [claro.data.error :refer [with-error?]]
            [claro.data.protocols :as p]
            [claro.data.ops.then :refer [then then!]]
            [claro.data.ops.chain :refer [chain-eager chain-when]]))

;; ## Record

(defn- project-match
  [condition->template else-template value partial-value]
  (second
    (or (some
          (fn [[condition template]]
            (if (condition partial-value)
              [:done (pr/project template value)]))
          condition->template)
        (if else-template
          [:done (pr/project else-template value)]))))

(defrecord ConditionalProjection [template
                                  condition->template
                                  else-template]
  pr/Projection
  (project [_ value]
    (with-error? value
      (-> (pr/project template value)
          (then!
            #(project-match condition->template else-template value %))))))

;; ## Constructors

(defn- make-conditional
  [partial-template condition template more]
  (let [pairs (cons [condition template] (partition 2 more))
        [c maybe-else-template] (last pairs)
        [condition->template else-template]
        (if (= c :else)
          [(butlast pairs) maybe-else-template]
          [pairs nil]) ]
    (->ConditionalProjection
      partial-template
      condition->template
      else-template)))

(defn conditional
  "Apply the first projection whose predicate matches the value resulting from
   projecting `partial-template`.

   ```
   (projection/conditional
     {:type projection/leaf}
     (comp #{:animal} :type) {:left-paw projection/leaf}
     (comp #{:human} :type)  {:left-hand projection/leaf})
   ```

   `:else` can be given to denote the default case:

   ```clojure
   (projection/conditional
     {:type projection/leaf}
     (comp #{:animal} :type) {:left-paw projection/leaf}
     :else                   {:left-hand projection/leaf})
   ```

   Note that, sometimes,  you can express this just as well using a [[bind]] or
   [[let]] projection:

   ```clojure
   (projection/let [{:keys [type]} {:type projection/leaf}]
     (case type
       :animal {:left-paw projection/leaf}
       :human  {:left-hand projection/leaf}))
   ```
   "
  [partial-template condition template & more]
  {:pre [(even? (count more))]}
  (make-conditional partial-template condition template more))

(defn conditional-union
  "Apply and merge all projections whose predicates match the value resulting
   from projecting `partial-template`.

   ```clojure
   (projection/conditional-union
     {:type          projection/leaf
      :has-children? projection/leaf}
     (comp #{:animal} :type) {:left-paw projection/leaf}
     (comp #{:human} :type)  {:left-hand projection/leaf}
     :has-children?          {:children [{:name projection/leaf}]})
   ```

   The matching projections have to produce maps with disjunct sets of keys."
  [partial-template condition template & more]
  {:pre [(even? (count more))]}
  (->> (partition 2 more)
       (map
         (fn [[condition template]]
           (conditional partial-template condition template)))
       (cons (conditional partial-template condition template))
       (union*)))
