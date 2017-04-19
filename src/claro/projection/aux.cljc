(ns claro.projection.aux
  (:require [claro.projection
             [objects :refer [leaf]]
             [transform :refer [transform]]]))

(defn ^{:added "0.2.13"} extract-in
  "Extract a subtree/leaf located under the given path.

   ```clojure
   (-> {:sherlock (->Person 1)}
       (projection/apply (extract-in [:sherlock :name]))
       (engine/run!!))
   ;; => \"Sherlock\"
   ```

   For non-leaf values, a template can be given that will be applied before
   extraction."
  ([template ks]
   (transform #(get-in % ks) template))
  ([ks]
   (extract-in (assoc-in {} ks leaf) ks)))

(defn extract
  "Extract a subtree/leaf located under the given key.

   ```clojure
   (-> (->Person 1)
       (projection/apply (extract :name))
       (engine/run!!))
   ;; => \"Sherlock\"
   ```

   For non-leaf values, a template can be given that will be applied before
   extraction."
  ([template k] (extract-in template [k]))
  ([k] (extract-in [k])))
