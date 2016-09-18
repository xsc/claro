# Engine

So far, we've only used `claro.engine/run!!` to resolve our values. But claro's
engine offers a lot more.

First off, both [[run!!]] and [[run!]] use the default engine but you can build
a custom one using [[claro.engine/engine]]. Engines implement `IFn`, so they can
be called like functions.

```clojure
(defonce run-engine
  (engine/engine ...))

@(run-engine (->Person 1))
```

The next sections describe the moving/customizable parts.

### Deferred Values

Instead of blocking during resolution, [[run!]] produces a deferred value:

```clojure
(engine/run! (->Person 1))
;; => << … >>
```

The default implementation uses [Manifold][1], so you can set execution timeouts
or register error handlers:

```clojure
(-> (->Person 1)
    (engine/run!)
    (d/catch
      (fn [_]
        ::error))
    (d/timeout! 1000 ::timeout))
```

To use a different implementation (e.g. the one provided in
`claro.runtime.impl.core-async`), use the two-parameter engine constructor:

```clojure
(defonce run-engine
  (engine/engine
    claro.runtime.impl.core-async/impl
    {}))
```

This will work for any `Resolvable` returning a core.async channel.

> __Note:__ The implementation used by an engine can be accessed using the
> [[impl]] function.

[1]: https://github.com/ztellman/manifold

### Environment

Meaningful data access without configuration pointing at a datasource is rare,
so it is necessary for `Resolvable` values to be aware of said configuration.
There are multiple possibilities:

- store it in global vars,
- store it in dynamic vars and use `binding` around the resolution call,
- store it in the `Resolvable` record.

These are viable options for claro, too, but the preferred way would be to bind
an engine to your environment, using the `:env` key:

```clojure
(defonce run-engine
  (engine/engine
    {:env {:db {:subprotocol "postgresql", ...}}}))
```

The environment will be passed as the second parameter to both [[resolve!]] and
[[resolve-batch!]] and can contain things like datastore connections, a user to
scope resolution too, etc ...

Parts of the environment can be added or replaced when calling the engine:

```clojure
(run-engine
  (->Person 1)
  {:env {:db {:subprotocol "mysql", ...}}})
```

### Selector

During each iteration, the resolution engine selects a set of resolvables to
process. By default, it attempts to resolve all availble values, but this
behaviour can be adjusted by supplying a different [[Selector]] when creating
the engine:

```clojure
(defonce run-engine
  (engine/engine
    {:selector (claro.data.selector/parallel-selector 2)}))
```

Just like `:env` this can be overridden on a per-call basis, so you are able to
use specialized selection for cases where you need it.

See the [[claro.engine.selector]] namespace for more variants.

### Middlewares

The resolver function can be wrapped with custom middlewares using
[[claro.engine/wrap]]. It takes two parameters: the environment and the batch of
resolvables – all of the same class – and produces a deferred value.

For example, we can write a middleware that attaches a timeout to each single
`Resolvable` batch:

```clojure
(defn wrap-timeout
  [engine timeout-ms]
  (->> (fn [resolver]
         (fn [env batch]
           (-> (resolver env batch)
               (d/timeout! timeout-ms))))
       (engine/wrap engine)))
```

More possibilities include caching, tracing, monitoring, circuit-breaking,
etc... I recommend checking out the existing middlewares (in
`claro.middleware.*`) for more examples.
