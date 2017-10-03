(ns claro.projection.sort
  (:refer-clojure :exclude [sort-by])
  (:require [claro.projection
             [protocols :as p]
             [transform :as transform]]
            [claro.data
             [error :refer [with-error?]]
             [ops :as ops]]))

(deftype SortProjection [sort-template output-template]
  p/Projection
  (project [_ original]
    (with-error? original
      (->> (fn [sq]
             (-> (transform/transform
                   (fn [sort-keys]
                     (->> (map vector sort-keys sq)
                          (clojure.core/sort-by first)
                          (mapv second)))
                   [sort-template]
                   output-template)
                 (p/project sq)))
           (ops/then original)))))

(defmethod print-method SortProjection
  [^SortProjection value ^java.io.Writer w]
  (.write w "#<claro/sort-by ")
  (print-method (.-sort-template value) w)
  (when-let [out (.-output-template value)]
    (.write w " -> ")
    (print-method out w))
  (.write w ">"))

(defn ^{:added "0.2.19"} sort-by
  "A projection sorting the sequence that's currently being resolved.
   `sort-template` is applied to each element of the sequence to generate
   a value to sort by, while `output-template´ is used to further project
   the resulting sorted sequence.

   ```clojure
   (-> [{:index 3, :value 'third}
        {:index 1, :value 'first}
        {:index 2, :value 'second}]
       (projection/apply
         (projection/sort-by
           (projection/extract :index)
           [{:value projection/leaf}]))
       (engine/run!!))
   ;; => [{:value 'first}, {:value 'second}, {:value 'third}]
   ```

   If no `output-template` is given, the resulting tree may not be infinite
   (or a further projection has to be applied externally)."
  ([sort-template]
   (->SortProjection sort-template nil))
  ([sort-template output-template]
   {:pre [(some? sort-template)
          (some? output-template)]}
   (->SortProjection sort-template output-template)))
