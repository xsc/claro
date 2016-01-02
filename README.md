# claro

__claro__ is a library for simple, efficient and elegant access to remote data
sources focusing on usability, testability and overall fn.

It is inspired by [muse][muse] which is awesome in its own right and will be a
better fit for some applications - just as claro will be for others.

[muse]: https://github.com/kachayev/muse

## Usage

Don't.

## Overview

### Resolvables

In claro, you define `Resolvable`s that encapsulate I/O and transformation
logic, allowing composition of resolvables in a concise manner:

```clojure
(require '[claro.data :as data]
         '[manifold.deferred :as d]))

(def fetch-color! (constantly {:name "white"}))
(def fetch-house! (constantly {:colour_id 3, :street "221B Baker Street"}))

(defrecord ColourString [id]
  data/Resolvable
  (resolve! [_ env]
    (d/future
      (-> id (fetch-color! env) :name))))

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
;; => {:sherlock {:id 221, ...}, :watson   {:id 221, ...}}
```

Resolution engines also allow for customization through middlewares - something
that will be outlined, together with more details, in
[Engine Capabilities](#engine-capabilities).

## Resolvable Capabilities

### Manifold

Claro relies on the [manifold](https://github.com/ztellman/manifold) library for
representation of asynchronous logic. This means that `Resolvables` can return:

- Clojure's `future`, `delay`, `promise`,
- Manifold's own `deferred` values,
- `java.util.concurrent.Future`s (e.g. from `ExecutorService.submit()`),
- or just plain values (whose computation will block resolution, though).

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

To transform resolvables, you can wrap them using claro's composition functions.

#### Blocking Composition (`wait`)

`claro.data/wait` will apply one or more functions to a __fully-resolved__
value, meaning that it should not be used on potentially infinite resolvable
trees (see next section). Which, in turn, means that its use should be avoided
as much as possible.

```clojure
(-> (ColourString. 0)
    (data/wait
      (fn [colour-name]
        {:name colour-name, :count (count color-name)}))
    (engine/run!!))
;; => {:name "white", :count 5}
```

(Note: `engine/run!!` is resolution + dereferencing using the default engine.)

#### Conditional Composition (`chain`, `chain-*`)

TODO

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

## Engine Capabilities

TODO.

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
