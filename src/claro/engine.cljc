(ns claro.engine
  "Main resolution and engine logic.

   Claro's resolution engine is powerful and customizable, allowing for flexible
   introspection and manipulation of every resolution run.

   See the [Engine][1] documentation for details and examples.

   [1]: 03-engine.md
   "
  (:refer-clojure :exclude [run!])
  (:require [claro.engine
             [core :as core]
             [adapter :as adapter]
             [selector :as selector]
             [protocols :as p]]
            #?(:clj [claro.runtime.impl.manifold :as default])
            #?(:cljs [claro.runtime.impl.core-async :as default])
            [potemkin :refer [import-vars]]))

;; ## Engine Constructors/Runners

(def default-impl
  "The default deferred implementation used for resolution."
  default/impl)

(def default-opts
  "The default engine options."
  {:env         {}
   :selector    selector/default-selector
   :adapter     adapter/default-adapter
   :max-batches 32})

(def ^:private default-engine
  "The pre-prepared default engine."
  (core/build default-impl default-opts))

(defn engine
  "Create a new resolution engine, based on the following options:

   - `:env`: a value that is passed as the `env` parameter to `Resolvable`s'
   `resolve!` and `resolve-batch!` functions (default: `{}`),
   - `:adapter`: a function that will be called to run calls to `resolve!` and
   `resolve-batch!`,
   - `:selector`: a `claro.engine.selector/Selector` implementation used during
   each iteration to decide what to resolve next,
   - `:max-batches`: a value describing how many iterations are allowed before
   the engine will throw an `IllegalStateException` (default: `32`).

   The resulting value's resolution behaviour can be wrapped using
   `claro.engine/wrap`."
  ([] default-engine)
  ([opts]
   (if (empty? opts)
     default-engine
     (engine default-impl opts)))
  ([impl opts]
   (core/build impl (merge default-opts opts))))

(defn run!
  "Resolve the given value using an engine created on-the-fly. See
   `claro.engine/engine` for available options. Immediately returns a
   deferred."
  ([value] (default-engine value))
  ([opts value] ((engine opts) value)))

(defn run!!
  "Resolve the given value using an engine created on-the-fly. See
   `claro.engine/engine` for available options. Blocks until the resolved
   value has been obtained."
  ([value] @(default-engine value))
  ([opts value] @(run! opts value)))

;; ## Middlewares

(import-vars
  [claro.engine.core
   impl
   wrap])
