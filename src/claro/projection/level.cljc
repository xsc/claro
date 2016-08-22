(ns claro.projection.level
  (:require [claro.projection.protocols :as pr]
            [claro.data.ops
             [collections :as c]
             [then :refer [then]]]))

;; ## Record

(declare ->LevelProjection)

(defn- project-map
  [m current-level]
  (if (pos? current-level)
    (let [template (->LevelProjection (dec current-level))
          project  #(pr/then-project template %)]
      (->> (for [[k v] m]
             [(project k) (project v)])
           (into {})))
    {}))

(defn- project-coll
  [c current-level]
  (if (pos? current-level)
    (let [template (->LevelProjection current-level)]
      (c/map-single #(pr/project template %) c))
    (empty c)))

(defn- project-level
  [value current-level]
  (cond (not (coll? value)) value
        (map? value)        (project-map value current-level)
        :else               (project-coll value current-level)))

(defrecord LevelProjection [n]
  pr/Projection
  (project [_ value]
    (then value #(project-level % n))))

;; ## Constructor

(defn levels
  "Generate Projection template representing the first `n` levels of a value.
   Leafs up to the given level will be maintained.

   E.g. for the following value:

       {:a 0
        :b {:c 1}
        :d {:e [{:f 2}]}}

   Result for `n` == 1:

       {:a 0, :b {}, :d {}}

   Result for `n` == 2;

       {:a 0, :b {:c 1}, :d {:e []}}

   For `n` >= 3, the full value will be returned."
  [n]
  {:pre [(pos? n)]}
  (->LevelProjection n))
