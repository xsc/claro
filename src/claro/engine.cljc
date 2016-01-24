(ns claro.engine
  (:refer-clojure :exclude [run!])
  (:require [claro.engine
             [builder :as builder]
             [protocols :as p]]
            [claro.engine.middlewares
             [override :as override]
             [trace :as trace]]
            #?(:clj [claro.runtime.impl.manifold :as default])
            #?(:cljs [claro.runtime.impl.core-async :as default])
            [potemkin :refer [import-vars]]))

;; ## Engine Constructors/Runners

(def ^:private default-engine
  "The pre-prepared default engine."
  (builder/build default/impl {}))

(defn engine
  "Create a new resolution engine, based on the following options:

   - `:env`: a value that is passed as the `env` parameter to `Resolvable`s'
   `resolve!` and `resolve-batch!` functions (default: `{}`),
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
     (builder/build default/impl opts)))
  ([impl opts]
   (builder/build impl opts)))

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
  [claro.engine.protocols
   wrap-resolver
   wrap-selector]
  [claro.engine.middlewares.override
   override
   overrides]
  [claro.engine.middlewares.trace
   tracing])
