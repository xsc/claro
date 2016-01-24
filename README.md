# claro

__claro__ is a library for simple, efficient and elegant access to remote data
sources focusing on usability, testability and overall fn.

[![Build Status](https://travis-ci.org/xsc/claro.svg?branch=master)](https://travis-ci.org/xsc/claro)

It is inspired by [muse][muse] which is awesome in its own right and will be a
better fit for some applications - just as claro will be for others.

[muse]: https://github.com/kachayev/muse

## Usage

Don't.

__Leiningen__ ([via Clojars][clojars])

[![Clojars Project](https://img.shields.io/clojars/v/claro.svg)][clojars]

This library requires Clojure >= 1.7.0.

[clojars]: https://clojars.org/claro

## Overview

### Resolvables

In claro, you define `Resolvable`s that encapsulate I/O and transformation
logic, allowing composition of resolvables in a concise manner:

```clojure
(require '[claro.data :as data]
         '[manifold.deferred :as d])

(def fetch-colour! (constantly {:name "white"}))
(def fetch-house! (constantly {:colour_id 3, :street "221B Baker Street"}))

(defrecord ColourString [id]
  data/Resolvable
  (resolve! [_ env]
    (d/future
      (-> id (fetch-colour! env) :name))))

(defrecord House [id]
  data/Resolvable
  (resolve! [_ env]
    (d/future
      (let [{:keys [colour_id street]} (fetch-house! id env)]
        {:id     id
         :colour (ColourString. colour_id)
         :street street}))))
```

Note that `House` contains a `ColourString` resolvable to reference its colour's
name. See [Resolvable Capabilities](#resolvable-capabilities) for a detailed
overview of what you get if you buy into them.

### Resolution Engine

To resolve a value, one has to employ a _resolution engine_ - which is basically
just a function taking a `Resolvable` and producing a deferred value. It is also
(optionally) bound to an environment (see the `env` parameter above):

```clojure
(require '[claro.engine :as engine])

(def resolve!
   (engine/engine
     {:env {:db {:host ...}}}))
```

Application of the function (+ dereferencing) yields the resolved value:

```clojure
@(resolve! (ColourString. 3)) ;; => "white"
@(resolve! (House. 221))      ;; => {:id 221, :colour "white", ...}
```

Note that the input to an engine does not have to be a resolvable but can be
any value built upon one or more of them:

```clojure
@(resolve! {:sherlock (House. 221), :watson (House. 221)})
;; => {:sherlock {:id 221, ...}, :watson {:id 221, ...}}
```

Resolution engines also allow for customization through middlewares - something
that will be outlined, together with more details, in
[Engine Capabilities](#engine-capabilities).

## Resolvable Capabilities

### Manifold

Claro, by default, relies on the [manifold][manifold] library for representation
of asynchronous logic. This means that `Resolvables` can return:

- Clojure's `future`, `delay`, `promise`,
- Manifold's own `deferred` values,
- `java.util.concurrent.Future`s (e.g. from `ExecutorService.submit()`),
- or just plain values (whose computation will block resolution, though).

Note that claro lets you [plug in any deferred
implementation](#pluggable-deferred-implementation) you desire (e.g.
`core.async` channels).

[manifold]: https://github.com/ztellman/manifold

### Batched Resolvables

Optimizing for the resolution of multiple values of the same class, you can
declare batchwise resolution logic by implementing the `BatchedResolvable`
protocol (in addition to `Resolvable`, mind):

```clojure
(defrecord ColourString [id]
  data/Resolvable
  data/BatchedResolvable
  (resolve-batch! [_ env colours]
    (d/future
      (mapv (comp :name #(fetch-colour! % env) :id) colours))))
```

`resolve-batch!` has to return a seq (or a deferred with a seq) with resolution
results matching the input order. It must contain at least as many elements as
requested, but may return more - even infinitely so.

### Composition

To transform resolvables, you can wrap them using claro's low-level composition
functions. There is also a set of collection manipulation facilities that
are described in [Collection Operations](#collection-operations).

#### Blocking Composition (`then!`)

`claro.data/then!` will apply a transformation to a __fully-resolved__
value, meaning that it should not be used on potentially infinite resolvable
trees (see next section). Which, in turn, means that its use should be avoided
as much as possible.

```clojure
(-> {:name (ColourString. 0)}
    (data/then!
      (fn [{:keys [name]}]
        {:name name, :class (class name)}))
    (engine/run!!))
;; => {:name "white", :class java.lang.String}
```

(Note: `engine/run!!` is resolution + dereferencing using the default engine.)

#### Eager Composition (`then`)

Most of the time, transformations can be applied before a value is fully
resolved. For example, to get the first element of a seq of resolvables, it
doesn't matter (result-wise) whether you call `first` on the original seq or
the resolved one - and actually, removing elements whose resolution results will
never be used sounds like a good idea, doesn't it?

`claro.data/then` will apply a transformation to any partially resolved value,
i.e. one that is neither a `Resolvable` nor wrapped inside another composition.

```clojure
(-> {:name (ColourString. 0)}
    (data/then
      (fn [{:keys [name]}]
        {:name name, :class (class name)}))
    (engine/run!!))
;; => {:name "white", :class user.ColourString}
```

As you can see, the function was called on the `ColourString` value, as opposed
to the resolved `java.lang.String` from the previous example.

`then` should only perform top-level transformation functions (i.e. add/remove a
key, wrap a value, ...) and avoid operations on nested values (since they might
not-yet be resolved). For example, the following might not behave as expected:

```clojure
(-> {:name (ColourString. 0)}
    (data/then update :name count)
    (engine/run!!))
;; => {:name 1}
```

`count` was called on the `ColourString` record, not its resolved value.

#### Conditional Composition (`on`)

As seen in the previous section, eager composition might make it hard to reason
about the structure of data one is operating on. Claro tries to mitigate this
problem by offering a way to _guard_ transformations with a predicate.

`claro.data/on` will only run a transformation if the predicate is fulfilled and
_throw an exception_ if a value was fully resolved without triggering the
transformation.

```clojure
(-> {:name (ColourString. 0)}
    (data/on #(-> % :name string?) update :name count)
    (engine/run!!))
;; => {:name 5}

(-> {:name [(ColourString. 0)]}
    (data/on #(-> % :name string?) update :name count)
    (engine/run!!))
;; => IllegalStateException: predicate ... does not hold for fully resolved: {:name ["white"]}
```

Note that in this case, the preferred solution would be to use claro's built in
[collection functions](#collection-operations), e.g.: `claro.data/update`:

```clojure
(-> {:name (ColourString. 0)}
    (data/update :name count)
    (engine/run!!))
;; => {:name 5}
```

#### Combination (`fmap`, `fmap!`, `fmap-on`)

To apply a function to one or more potentially resolvable values, use
`claro.data/fmap`.

```clojure
(engine/run!! (data/fmap str (ColourString. 0) " == " (ColourString. 1)))
;; => "white == white"
```

Just as before, eager resolution might mean that the function is applied before
the data has reached the shape it might expect:

```clojure
(engine/run!! (data/fmap (comp count :name) {:name (ColourString. 0)}))
;; => 1
```

`fmap-on` will guard execution using a given condition:

```clojure
(engine/run!!
  (data/fmap-on
    #(-> % :name string?)
    (comp count :name)
    {:name (ColourString. 0)}))
;; => 5
```

And finally, `fmap!` will wait for all parameters to be fully resolved:

```clojure
(engine/run!! (data/fmap! (comp count :name) {:name (ColourString. 0)}))
;; => 5
```

### Infinite Trees + Projection

Since resolvables may directly reference other resolvables, one can build
potentially infinite trees, usually either triggering the engine's maximum depth
protection or a `StackOverflowError`. Using a _projection template_ one can "cut
off" those parts of the tree that there is no interest in.

```clojure
(defrecord InfiniteSeq [n]
  data/Resolvable
  (resolve! [_ _]
    {:head n, :tail (InfiniteSeq. (inc n))}))

(engine/run!! (InfiniteSeq. 0)) ;; => IllegalStateException
```

Let's see what the `:head` of the initial `:tail` is:

```clojure
(engine/run!!
  (data/project
    (InfiniteSeq. 0)
    {:tail {:head nil}}))
;; => {:tail {:head 1}}
```

Or one level deeper:

```clojure
(engine/run!!
  (data/project
    (InfiniteSeq. 0)
    {:tail {:tail {:head nil}}}))
;; => {:tail {:tail {:head 2}}}
```

Note that projection is an experimental feature and might yield unexpected
results in some cases.

### Collection Operations

The namespace `claro.data` contains operations mirroring their standard Clojure
counterparts but handling eager/conditional composition in a way that lets them
be used with potentially infinite trees and thus projection (outlined above).

__Sequence Operations__

- `drop`
- `first`
- `map`
- `nth`
- `take`

__Map Operations__

- `assoc`
- `assoc-in`
- `get`
- `get-in`
- `select-keys`
- `update`
- `update-in`

Most of the time, when only manipulating the top-level structure of values, a
combination of `then` and the desired operation will be enough.

## Engine Capabilities

### Environment

Meaningful data access without configuration pointing at a datasource is rare,
so it is necessary for `Resolvable`s to be aware of said configuration. There
are multiple possibilities:

- store it in global vars,
- store it in dynamic vars and use `binding` around the resolution call,
- store it in the `Resolvable` record.

These are viable options for claro, too, but the preferred way would be to bind
an engine to your environment, using the `:env` key:

```clojure
(def resolve! (engine/engine {:env {:global-constant 0}}))
```

This value will be passed as-is to your `Resolvables` and could contain anything
ranging from DB clients to some in-memory caching atom:

```clojure
(defrecord FromEnv [k]
  data/Resolvable
  (resolve! [_ env]
    (get env k)))

@(resolve! (FromEnv. :global-constant))
;; => 0
```

You can extend/override the environment when calling the engine:

```clojure
@(resolve! (FromEnv. :global-constant) {:env {:global-constant 1}})
;; => 1
```

This lets you pass dynamic information, e.g. authentication/authorization
data to the engine, without having to create a new instance.

### Testing

Resolution of a single `Resolvable` class can be mocked using
`claro.engine/override`, which takes a class and a single-arity resolution
function:

```clojure
(def resolve!
  (-> (engine/engine)
      (engine/override ColourString (constantly "red"))))

@(resolve! (House. 221))
;; => {:id 221, :colour "red", :street "221B Baker Street"}
```

`claro.engine/overrides` can be used to add overrides based on a class/resolver
map.

### Tracing Middleware

For debugging purposes, you can let the engine print out each resolution step,
which lets you analyze how much of what gets resolved in which order:

```clojure
(def resolve! (-> (engine/engine) (engine/trace-stats)))

@(resolve! (House. 221))
;; [user.House] 1 of 1 elements resolved ... 0.006s
;; [user.ColourString] 1 of 1 elements resolved ... 0.001s
;; => {:id 221, :colour "white", :street "221B Baker Street"}
```

### Custom Middlewares

When running the engine on a value, it repeatedly performs the following steps:

1. __Inspection__: Collect all remaining `Resolvable` values.
2. __Selection__: Select the `Resolvable` class(es) to resolve in this
   iteration (default: all of them).
3. __Resolution__: Resolve the selected batches by calling the respective
   `Resolvable` protocol functions.
4. __Application__: Insert the resolved values into the original tree.

Inspection and application is handled by claro's internal tree representation,
but selection and resolution are customizable.

The selector takes and returns a seq of classes and can be wrapped using
`claro.engine/wrap-selector` - to e.g. only select `n` different `Resolvable`
classes per iteration:

```clojure
(defn wrap-max-selection
  [engine n]
  (engine/wrap-selector
    engine
    #(comp (partial take n) %)))
```

The resolver takes the environment value and a seq of `Resolvable`s and has to
return another, in-order seq of (at least) the same length. It can be wrapped
using `claro.engine/wrap-resolver` - to e.g. collect some resolution stats in an
atom:

```clojure
(defn wrap-stats
  [engine stats-atom]
  (engine/wrap-resolver
    engine
    (fn [resolver]
      (fn [env [v :as batch]]
        (swap! stats-atom update (class v) (fnil + 0) (count batch))
        (resolver env batch)))))
```

claro thus allows you to easily hook into _what_ will be resolved and _how_ the
eventual resolution will be performed.

### Pluggable Deferred Implementation

Claro's runtime is independent of the actual deferred implementation, allowing
you to plug in any one you desire. A full-fledged implementation for [core.async
channels][core-async] is already included and can be passed to
`claro.engine/engine` for it to be used:

```clojure
(require '[claro.runtime.impl.core-async :as core-async]
         '[claro.engine :as engine]
         '[claro.data :as data]
         '[clojure.core.async :refer [go timeout <! <!!]])

(def resolve! (engine/engine core-async/impl {:env {}}))

(defrecord ChannelResolvable [timeout-ms x]
  data/Resolvable
  (resolve! [_ _]
    (go
      (<! (timeout timeout-ms))
      x)))

(<!! (resolve! (ChannelResolvable. 100 :x)))
;; => :x
```

Note that you have to explicitly include the [core.async][core-async]
dependency.

[core-async]: https://github.com/clojure/core.async

## License

```
The MIT License (MIT)

Copyright (c) 2015-2016 Yannick Scherer

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
