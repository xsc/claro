(ns claro.projection.aux
  (:require [claro.projection
             [objects :refer [leaf]]
             [transform :refer [transform]]]))

(defn extract
  "Extract a single leaf value from a subtree.

   ```clojure
   (-> (->Person 1)
       (projection/apply (extract :name))
       (engine/run!!))
   ;; => \"Sherlock\"
   ```
   "
  ([template k]
   (transform #(get % k) template leaf))
  ([k]
   (extract {k leaf} k)))
