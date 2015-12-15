# claro

So far, this is [muse][muse] with some experimental stuff.

[muse]: https://github.com/kachayev/muse

## Usage

Don't.

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
