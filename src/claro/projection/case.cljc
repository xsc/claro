(ns claro.projection.case
  (:refer-clojure :exclude [case])
  (:require [claro.projection.protocols :as pr]
            [claro.data.protocols :as p]))

;; Helpers

(defn- throw-case-mismatch!
  [value class->template]
  (throw
    (IllegalArgumentException.
      (format
        (str "no match in 'case' projection.%n"
             "value: %s%n"
             "cases: %s")
        (pr-str value)
        (pr-str (vec (keys class->template)))))))

(defn- assert-resolvable!
  [value class->template]
  (when-not (p/resolvable? value)
    (throw
      (IllegalArgumentException.
        (format
          (str "'case' projection can only be applied to resolvable.%n"
               "value: %s%n"
               "cases: %s")
          (pr-str value)
          (pr-str (vec (keys class->template))))))))

;; ## Record

(defrecord CaseProjection [class->template]
  pr/Projection
  (project [_ value]
    (assert-resolvable! value class->template)
    (let [template (get class->template
                        (class value)
                        (:else class->template ::none))]
      (if (not= template ::none)
        (pr/project template value)
        (throw-case-mismatch! value class->template)))))

;; ## Constructor

(defn- collect-cases
  [pairs]
  (->> (for [[classes template] pairs
             class (if (sequential? classes)
                     classes
                     [classes])]
         [class template])
       (reduce
         (fn [result [^Class class template]]
           (when (contains? result class)
             (throw
               (IllegalArgumentException.
                 (str "duplicate in 'case' projection: " (.getName class)))))
           (assoc result class template)) {})))

(defn case
  "Dispatch on the class of a `Resolvable`, applying the respective template."
  [class template & more]
  {:pre [(even? (count more))]}
  (->> (partition 2 more)
       (cons [class template])
       (collect-cases)
       (->CaseProjection)))
