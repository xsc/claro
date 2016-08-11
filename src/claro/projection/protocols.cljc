(ns claro.projection.protocols
  (:require [claro.data.ops.then :refer [then]]))

(defprotocol Projection
  "Protocol for projection templates."
  (project-template [template value]
    "Use the given template to ensure the shape of the given value."))

(defn then-project-template
  "Use the given projection template to ensure the shape of the, potentially
   not fully resolved value."
  [template value]
  (then value #(project-template template %)))
