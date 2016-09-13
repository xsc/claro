(ns claro.engine.selector)

(defprotocol Selector
  (instantiate [selector]
    "Generate a selector instance, i.e. a function taking a map of
     classes/resolvables and returning a seq of classes to resolve during
     the current iteration step."))

(def default-selector
  "Always selects all available classes."
  (reify Selector
    (instantiate [_]
      keys)))
