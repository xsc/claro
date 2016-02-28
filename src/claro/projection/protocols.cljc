(ns claro.projection.protocols)

(defprotocol Projection
  "Protocol for projection templates."
  (project-template [template value]
    "Use the given template to ensure the shape of the given value."))
