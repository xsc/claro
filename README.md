# claro

So far, this replicates [muse][muse]'s functionality and adds some experimental
stuff.

[muse]: https://github.com/kachayev/muse

## Usage

Don't.

## Differences to Muse

### Manifold

Claro relies on the [manifold](https://github.com/ztellman/manifold) library for
representation of asynchronous logic. This means that `Resolvables` can return:

- Clojure's `future`, `delay`, `promise`,
- `core.async` channels,
- Manifold's own `deferred` values,
- or just plain values (whose computation will block resolution, though).

### Compound Resolvables

Muse's `DataSource`s have to produce fully resolved values, meaning that they
cannot generate a partial result where e.g. a single field points at another
`DataSource`. One can work around this limitation by calling `muse.core/run!`
within the source, but I'm not sure if this would be idiomatic use of the
library.

In claro, you can specify the following:

```clojure
(defrecord Friend [id]
  claro.data/Resolvable
  (resolve! [_ _]
    {:friend id}))

(defrecord Friends [id]
  claro.data/Resolvable
  (resolve! [_ _]
    (future
      (let [friend-ids [5 6 7 8]]
        (map #(Friend. %) friend-ids)))))
```

And resolution will produce:

```clojure
(let [run! (claro.data/engine)]
  @(run! (Friends. 1)))
;; => ({:friend 5} {:friend 6} {:friend 7} {:friend 8})
```

Of course, this can be used to generate potentially infinite trees, which is why
the next sections are particularly important.

### Projection (experimental)

Given a potentially infinite tree and a _projection template_, we can "cut off"
subtrees that do not match the template.

```clojure
(require '[claro.data :as data])

(defrecord Person [id]
  data/Resolvable
  (resolve! [_ _]
    {:id id
     :father (Person. (* (inc id) 3))
     :mother (Person. (* id 5))}))

(let [run! (data/tracing-engine)]
  @(run!
     (data/project
       (Person. 1)
       {:father {:mother {:id nil}}})))
;; [user.Person] 1 of 1 elements resolved ... 0.001s
;; [user.Person] 1 of 1 elements resolved ... 0.000s
;; [user.Person] 1 of 1 elements resolved ... 0.000s
;; => {:father {:mother {:id 30}}}
```

As you can see due to the (for-debug-purposes-only) tracing resolution engine,
there were three batches of resolutions, each with only one element:

- the initial person,
- the initial person's father,
- as well as the initial person's father's mother.

Contrast this with:

```clojure
(let [run! (data/tracing-engine)]
  @(run!
     (data/project
       (Person. 1)
       {:father {:mother {:id nil}}
        :mother {:father {:id nil}}})))
;; [user.Person] 1 of 1 elements resolved ... 0.002s
;; [user.Person] 2 of 2 elements resolved ... 0.000s
;; [user.Person] 2 of 2 elements resolved ... 0.000s
;; => {:father {:mother {:id 30}}, :mother {:father {:id 18}}}
```

We didn't change the resolution logic, just the projection template, causing
the inclusion of the initial person's mother's data.

## License

```
The MIT License (MIT)

Copyright (c) 2015 Yannick Scherer

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
