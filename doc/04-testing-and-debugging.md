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
