(ns claro.projection.union
  (:require [claro.projection.protocols :as pr]
            [claro.projection.conditional :refer [conditional]]
            [claro.data.ops
             [then :refer [then then!]]]))

;; ## Record

(defn- assert-disjunct-keys
  [maps]
  (when-let [ks (->> maps
                     (mapcat keys)
                     (frequencies)
                     (keep
                       (fn [[k c]]
                         (when (> c 1)
                           k)))
                     (seq))]
    (throw
      (IllegalStateException.
        (str "result maps for 'union' projection need to have disjunct keys "
             "but they overlap in " (pr-str (doall ks)) ": " (pr-str maps))))))

(defn- union-of-maps
  [maps]
  (assert-disjunct-keys maps)
  (into {} maps))

(defrecord UnionProjection [templates]
  pr/Projection
  (project-template [_ value]
    (-> (mapv #(pr/project-template % value) templates)
        (then! union-of-maps))))

;; ## Constructor

(defn union
  "Apply all projection templates to the value, merging them together into
   a final value. The templates have to produce maps with disjunct
   sets of keys."
  [templates]
  (->UnionProjection templates))

(defn conditional-union
  "Apply projection templates whose predicate matches the value. The matching
   templates have to produce maps with disjunct sets of keys."
  [condition template & more]
  (->> (partition 2 more)
       (map
         (fn [[condition template]]
           (conditional condition template)))
       (cons (conditional condition template))
       (union)))
