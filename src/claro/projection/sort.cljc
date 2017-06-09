(ns claro.projection.sort
  (:refer-clojure :exclude [sort-by])
  (:require [claro.projection
             [protocols :as p]
             [bind :as bind]
             [value :as value]
             [transform :as transform]]
            [claro.data
             [error :refer [with-error?]]
             [ops :as ops]]))

(defrecord SortProjection [sort-template output-template]
  p/Projection
  (project [_ original]
    (with-error? original
      (-> original
          (ops/then
            (fn [sq]
              (with-error? sq
                (-> (transform/transform
                      (fn [sort-keys]
                        (->> (map vector sort-keys sq)
                             (clojure.core/sort-by first)
                             (map second)))
                      [sort-template]
                      output-template)
                    (p/project sq)))))))))

(defn sort-by
  [sort-template & [output-template]]
  (->SortProjection sort-template output-template))
