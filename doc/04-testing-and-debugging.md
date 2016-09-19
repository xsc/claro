# Testing & Debugging

Claro has a focus on introspectability and testability, so it offers some
built-in ways of achieving both.

### Separation of Pure and Impure Logic

As outlined in [Basic Resolution](00-basics.md), you should use two protocols to
implement resolvables:

- `Resolvable` for impure logic, like I/O.
- `Transform` for pure logic, like transformations.

So, instead of writing the following:

```clojure
(defrecord Person [id]
  data/Resolvable
  (resolve! [_ env]
    (d/future
      (let [{:keys [friend-ids] :as person} (fetch-person! (:db env) id)]
        (-> person
            (assoc :friends (map ->Person friend-ids))
            (dissoc :friend-ids))))))
```

You should consider:

```clojure
(defrecord Person [id]
  data/Resolvable
  (resolve! [_ env]
    (d/future
      (fetch-person! (:db env) id)))

  data/Transform
  (transform [_ {:keys [friend-ids] :as person}]
    (-> person
        (assoc :friends (map ->Person friend-ids))
        (dissoc :friend-ids))))
```

Sure, it's a bit more verbose – but it also allows you to separately test your
transformation logic:

```clojure
(deftest t-person-transform
  (let [result (data/transform (->Person 1) {:id 1, :friend-ids [1 2 3]})]
    (is (= 1 (:id result)))
    (is (every? #(instance? Person %) (:friends result)))
    ...))
```

> __Note:__ While a similar result can surely be achieved by extracting each
> transformation into a separately testable function, you cannot guarantee that
> said function is really used by the `Resolvable`.

### Mocks

Another advantage of the approach described in the previous section is the fact
that you can easily mock the impure part of your `Resolvable` using
[[wrap-mock]].

For example, to try out a projection on a `Person` record we could mock the
respective query results:

```clojure
(def run-engine
  (-> (engine/engine)
      (wrap-mock
        Person
        (fn [{:keys [id]} env]
          {:id         id
           :name       "Person"
           :friend-ids [(inc id)]}))))
```

Which lets us do:

```clojure
(-> (->Person 1)
    (projection/apply {:friends [{:name projection/leaf}]})
    (run-engine)
    (deref))
```

Here's the thing: __Logic attached using the `Transform` protocol is still
run__, so if you want to craft a subtree with certain properties you have to
think about what query result conveys these properties. For instance, to produce
a person that has an empty `:friends` key your datastore has to return an empty
list of `:friend-ids`.

Note that there is also [[wrap-mock-result]] which will skip transformations and
just return whatever the function produces directly.

### Introspection

The namespace [[claro.middleware.observe]] contains multiple middlewares that
let you react to processing of single resolvables or resolvable batches,
optionally using a predicate or list of classes.

For example, to trace the result of every `Person` resolution, we could use:

```clojure
(defn trace-resolution
  [input output]
  (locking *out*
    (prn input '-> output)))

(def run-engine
  (-> (engine/engine)
      (wrap-observe-by-class [Person] trace-resolution)))
```

This will print a line every time we encounter a person.
