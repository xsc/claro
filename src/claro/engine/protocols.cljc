(ns claro.engine.protocols
  (:require [potemkin :refer [defprotocol+]]))

(defprotocol+ Engine
  "Protocol for a Resolution engine that supports wrapping of the
   batchwise-resolution fn."
  (wrap-resolver [engine wrap-fn]
    "Wrap the given engine's batchwise resolution fn using the given
     `wrap-fn`.")
  (wrap-selector [engine wrap-fn]
    "Wrap the given engine's selector function using the given `wrap-fn`.")
  (impl [engine]
    "Return the given engines deferred implementation."))
