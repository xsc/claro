(ns claro.projection.union
  (:refer-clojure :exclude [merge])
  (:require [claro.projection.protocols :as pr]
            [claro.data.error :refer [with-error?]]
            [claro.data.ops.chain :as chain]))

;; ## Record

;; ### Union

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
  (if (next maps)
    (do
      (assert-disjunct-keys maps)
      (into {} maps))
    (first maps)))

(deftype UnionProjection [templates]
  pr/Projection
  (project [_ value]
    (with-error? value
      (-> (mapv #(pr/project % value) templates)
          (chain/chain-blocking* union-of-maps)))))

(defmethod print-method UnionProjection
  [^UnionProjection value ^java.io.Writer w]
  (.write w "#<claro/union ")
  (print-method (vec (.-templates value)) w)
  (.write w ">"))

;; ### Merge

(defn- merge-of-maps
  [maps]
  (if (next maps)
    (into {} maps)
    (first maps)))

(deftype MergeProjection [templates]
  pr/Projection
  (project [_ value]
    (with-error? value
      (-> (mapv #(pr/project % value) templates)
          (chain/chain-blocking* merge-of-maps)))))

(defmethod print-method MergeProjection
  [^MergeProjection value ^java.io.Writer w]
  (.write w "#<claro/merge ")
  (print-method (vec (.-templates value)) w)
  (.write w ">"))

;; ## Constructor

(defn- with-grouped-templates
  "Group templates by plain maps and others, then call `f`."
  [templates f]
  (loop [templates templates
         maps      nil
         non-maps  nil]
    (if (seq templates)
      (let [[template & rst] templates]
        (if (and (map? template) (not (record? template)))
          (recur rst (conj maps template) non-maps)
          (recur rst maps (conj non-maps template))))
      (f maps non-maps))))

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
  {:pre [(seq templates)]}
  (if (next templates)
    (with-grouped-templates templates
      (fn [maps non-maps]
        (cond (and maps non-maps)
              (->UnionProjection
                (cons (union-of-maps maps) non-maps))

              maps
              (union-of-maps maps)

              :else
              (->UnionProjection non-maps))))
    (first templates)))

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

(defn ^{:added "0.2.5"} merge*
  "Like [[union*]] but will merge overlapping keys like `clojure.core/merge`."
  [templates]
  {:pre [(seq templates)]}
  (if (next templates)
    (->MergeProjection templates)
    (first templates)))

(defn ^{:added "0.2.5"} merge
  "Like [[union]] but will merge overlapping keys like `clojure.core/merge`."
  [& templates]
  (merge* templates))
