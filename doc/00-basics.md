# Basic Resolution

claro is a library that allows you to streamline your data access, providing
powerful optimisations and abstractions along the way.

```clojure
(require '[claro.data :as data]
         '[claro.engine :as engine]
         '[manifold.deferred :as d])
```

### Resolvables

We define our data access as records implementing the `Resolvable` protocol.

```clojure
(defrecord Person [id]
  data/Resolvable
  (resolve! [_ env]
    (d/future
      (fetch-person! (:db env) id))))
```

> __Note:__ You won't be able to use `extend-type`/`extend-protocol` with
> `Resolvable` – claro won't pick those values up since (for performance
> reasons) it checks for the _interface_, not the protocol.

Now, we can create an instance of our resolvable and retrieve the respective
value:

```clojure
(engine/run!! (->Person 1))
;; => {:id 1, :name "Sherlock Holmes"}
```

But Resolvables don't have to be top-level values, they can be anywhere within a
nested structure:

```clojure
(engine/run!! {:sherlock (->Person 1), :watson (->Person 2)})
;; => {:sherlock {:id 1, :name "Sherlock Holmes"}
;;     :watson   {:id 2, :name "John Watson"}}
```

And this way you can even write resolvables that produce other resolvables:

```clojure
(defrecord FriendsOf [id]
  data/Resolvable
  (resolve! [_ env]
    (d/future
      (->> (fetch-friend-ids! (:db env) id)
           (map ->Person)))))
```

> __Note:__ `d/future` creates a Manifold future but you can use any deferred
> value, e.g. Clojure futures or the result of `ExecutorService.submit()`.

### Pure vs. Impure Logic

To increase [testability][testing] it generally makes sense to separate your
impure logic (I/O) from your pure one (transformation of I/O results). To
facilitate this, claro lets resolvables implement the [[Transform]] protocol
which will be automatically be used to postprocess resolution results.

The previous example can thus be rewritten as:

```clojure
(defrecord FriendsOf [id]
  data/Resolvable
  (resolve! [_ env]
    (d/future
      (fetch-friend-ids! (:db env) id)))

  data/Transform
  (transform [_ friend-ids]
    (map ->Person friend-ids)))
```

> __Note:__ `transform` expects a single result as input, even if your
> `Resolvable` implements the batching mechanisms outlined in the next section.

While this split-up is completely optional, it is highly recommended. See the
topic [Testing & Debugging][testing] on how to best leverage it.

Note that there are two helper macros for common transformation types, namely
[[extend-transform]] and [[extend-list-transform]].

[testing]: 04-testing-and-debugging.md

### Batching

With our above records, resolving a `FriendsOf` record for someone that has a
hundred friends will produce a hundred `Person` queries. This is usually both
unwanted and unnecessary since data access can be batched.

You can tell claro that there is such a batchwise resolution logic by
additionally implementing the `BatchedResolvable` protocol:

```clojure
(defrecord Person [id]
  data/Resolvable
  data/BatchedResolvable
  (resolve-batch! [_ env people]
    (d/future
      (fetch-people! (:db env) (map :id people)))))
```

> __Note:__ Don't forget to also implement `Resolvable` – this is after all what
> claro takes to identify values of interest.

`resolve-batch!` gets a seq of all values to resolve (including the current one)
as its third parameter. It has to return results in an order matching the input,
e.g. the result seqs first element is the resolved value for the first
resolvable, and so on.

### Mutations

Nothing prevents you from performing side-effects in `resolve!` or
`resolve-batch!` but letting them run wild can produce unexpected results. This
is why you should always mark them with the `Mutation` protocol:

```clojure
(defrecord IncrementBy [n]
  data/Mutation
  data/Resolvable
  (resolve! [_ {:keys [counter]}]
    (swap! counter + n)))
```

claro imposes some healthy restrictions on mutations:

- They can only be used at the top-level, i.e. they cannot be returned from
  `resolve!` or `resolve-batch!`.
- There can only be one mutation per call to `engine/run!!` – the caller is
  responsible for execution order.
- If mutations and normal resolvables are mixed, the mutation will be run before
  any other values.

They are, however, normal resolvables when it comes to their return value.
