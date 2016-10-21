(ns claro.projection.aux
  (:require [claro.projection
             [objects :refer [leaf]]
             [transform :refer [transform]]]))

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
  ([template k]
   (transform #(get % k) template))
  ([k]
   (extract {k leaf} k)))
