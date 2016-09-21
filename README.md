# claro

__[Documentation](http://xsc.github.io/claro/)__ | __[Guides](doc)__

__claro__ is a library that allows you to streamline your data access, providing
powerful optimisations and abstractions along the way.

[![Build Status](https://travis-ci.org/xsc/claro.svg?branch=master)](https://travis-ci.org/xsc/claro)
[![Clojars Artifact](https://img.shields.io/clojars/v/claro.svg)](https://clojars.org/claro)

It is inspired by [muse][muse] and heavily influenced by [GraphQL][graphql].

claro requires Clojure ≥ 1.7.0.

[muse]: https://github.com/kachayev/muse
[graphql]: http://graphql.org/

## Features

claro is designed to be flexible and extensible. It'll make your data access
more elegant, efficient and testable, providing things like:

- __batched resolution__ of similar entities,
- automatic __caching__ of already known resolution results,
- __engine middlewares__ to hook into the resolution logic, allowing e.g.
  generic cache or circuit breaker implementations,
- pluggable __resolution strategies__ in the form of `Resolvable` selectors,
- an exchangeable __deferred implementation__, defaulting to
  [manifold][manifold],
- and pre-built middlewares for __data source mocking__, introspection and
  result transformation.

[manifold]: https://github.com/ztellman/manifold

## Quickstart

Data access is defined within records implementing the `Resolvable` interface.
Note that they can always produce more resolvables:

```clojure
(require '[claro.data :as data]
         '[manifold.deferred :as d])

(defrecord Person [id]
  data/Resolvable
  (resolve! [_ env]
    (d/future
      (fetch-person! (:db env) id)))
  data/Transform
  (transform [_ {:keys [friend-ids] :as person}]
    (assoc person :friends (map #(Person. %) friend-ids))))
```

Blindly resolving an infinite tree like this is usually not a good idea – but
claro offers _tree projections_ you can use to describe abstract transformations
of infinite structures:

```clojure
(require '[claro.projection :as projection])

(def person-with-friend-names
  {:id      projection/leaf
   :name    projection/leaf
   :friends [(projection/extract :name)]})
```

And here we go:

```clojure
(require '[claro.engine :as engine])

(engine/run!!
  (-> (->Person 1)
      (projection/apply person-with-friend-names)))
;; => {:id 1, :name "Sherlock Holmes", :friends ["Dr. Watson", "Ms. Hudson"]}
```

## Documentation

1. [Basic Resolution](doc/00-basics.md)
2. [Projections](doc/01-projection.md)
3. [Advanced Projections](doc/02-advanced-projection.md)
4. [Engine](doc/03-engine.md)
5. [Testing & Debugging](doc/04-testing-and-debugging.md)
6. [Implementation Notes](doc/99-notes.md)

All these topics are also available in claro's [auto-generated
documentation][codox].

[codox]: http://xsc.github.io/claro/

## Contributing

Contributions are always welcome. Please take a look at the [Contribution
Guidelines](CONTRIBUTING.md) for a quick overview of how your changes can best
make it to master.

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
