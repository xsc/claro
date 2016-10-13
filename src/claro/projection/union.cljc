(ns claro.projection.union
  (:require [claro.projection.protocols :as pr]
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
  (project [_ value]
    (-> (mapv #(pr/project % value) templates)
        (then! union-of-maps))))

;; ## Constructor

(defn union*
  "Apply all projection templates to the value, merging them together into
   a final value.

   ```clojure
   (projection/union
     [{:id projection/leaf}
      (projection/case-resolvable
        Zebra   {:number-of-stripes projection/leaf}
        Dolphin {:intelligence projection/leaf}
        :else   {})])
   ```

   Note that the the templates have to produce maps with disjunct sets of keys."
  [templates]
  (->UnionProjection templates))

(defn union
  "Syntactic sugar for [[union*]] allowing for projections-to-merge to be given
   as single parameters:

   ```clojure
   (projection/union
     {:id projection/leaf}
     (projection/case-resolvable
       Zebra   {:number-of-stripes projection/leaf}
       Dolphin {:intelligence projection/leaf}
       :else   {}))
   ```

   Note that the the templates have to produce maps with disjunct sets of keys."
  [& templates]
  (union* templates))
