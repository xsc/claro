(ns claro.projection.alias
  (:refer-clojure :exclude [alias])
  (:require [claro.projection.transform :refer [transform-at]]))

(defn alias
  "Copy the value at the given key to `alias-key`, using `rest-template`
   for further projection.

   This projection has to be applied to a map."
  [alias-key key rest-template]
  {:pre [(not= alias-key key)]}
  (transform-at key identity rest-template alias-key))
