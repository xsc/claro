(ns claro.projection.remove-nil
  (:require [claro.projection
             [protocols :as p]
             [maybe :refer [maybe]]
             [objects :refer [leaf]]]
            [claro.data.error :refer [with-error?]]
            [claro.data.ops
             [then :refer [then! then]]]))

;; ## Helpers

(def ^:private marker-seq-projection
  "A projection that generates a seq of booleans indicating which elements
   are nil (`true`) and which are not."
  (reify p/Projection
    (project [_ sq]
      (with-error? sq
        (->> (fn [sq]
               (map #(then % nil?) sq))
             (then sq))))))

(defn- rewrap-seq
  [original sq]
  (if (or (list? original) (seq? original))
    (list* sq)
    (into (empty original) sq)))

(defn- remove-nil-elements
  [original]
  (let [original' (seq original)]
    (-> (p/project marker-seq-projection original')
        (then!
          (fn [sq]
            (->> (map vector original' sq)
                 (keep
                   (fn [[value value-is-nil?]]
                     (if-not value-is-nil? value)))
                 (rewrap-seq original)))))))

;; ## Implementation

(defrecord RemoveNilProjection [template]
  p/Projection
  (project [_ original]
    (with-error? original
      (let [result (then original remove-nil-elements)]
        (if template
          (p/project template result)
          result)))))

;; ## Wrappers

(defn remove-nil-elements
  "A projection to remove all `nil` elements from a seq, before applying the
   given `template` to it. If no `template` is given, the seq without `nil`
   values will be returned directly (and needs to have another projection
   applied if infinite subtrees are possible)."
  [& [template]]
  (->RemoveNilProjection template))
