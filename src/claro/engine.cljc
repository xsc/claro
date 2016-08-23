(ns claro.engine
  (:refer-clojure :exclude [run!])
  (:require [claro.engine
             [core :as core]
             [adapter :as adapter]
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
   :selector    identity
   :adapter     adapter/default-adapter
   :max-batches 256})

(def ^:private default-engine
  "The pre-prepared default engine."
  (core/build default-impl default-opts))

(defn engine
  "Create a new resolution engine, based on the following options:

   - `:env`: a value that is passed as the `env` parameter to `Resolvable`s'
   `resolve!` and `resolve-batch!` functions (default: `{}`),
   - `:adapter`: a function that will be called to run calls to `resolve!` and
   `resolve-batch!`,
   - `:selector`: a function that, during each iteration, is given a seq of
   all currently resolvable classes and returns those that should be resolved
   in parallel (default: `identity`),
   - `:max-batches`: a value describing how many iterations are allowed before
   the engine will throw an `IllegalStateException` (default: `256`).

   The resulting value's resolution behaviour can be wrapped using
   `claro.engine/wrap-resolver` & co."
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
   wrap-resolver])
