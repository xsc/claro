# Advanced Projections

Sometimes, you need a bit more flexibility than the [basic projections][1] can
offer – and claro can provide that by allowing arbitrary, and even conditional,
transformations of your data.

[1]: 01-projection.md

### Dispatch on Resolvable or Result Class

There might be cases where you expect different kinds of `Resolvable` values to
appear at a certain position. For example, we might want to model a series of
different animals:

```clojure
(defrecord Tiger [id]
  data/Resolvable
  (resolve! [_ _]
    (d/future
      {:id                id
       :name              "Tiger"
       :number-of-stripes (* id 4)})))

(defrecord Zebra [id]
  data/Resolvable
  (resolve! [_ _]
    (d/future
      {:id                id
       :name              "Zebra"
       :number-of-stripes (* id 4)})))

(defrecord Dolphin [id]
  data/Resolvable
  (resolve! [_ _]
    (d/future
      {:id                id
       :name              "Dolphin"
       :intelligence      (* id 40)})))
```

Assuming we have a mixed seq of these animal `Resolvable` values, we certainly
can easily create a projection to retrieve `:id` and `:name` – but how do we
handle the animal specific fields like `:number-of-stripes` and `:intelligence`?

[[case-resolvable]] dispatches on the `Resolvable` class, so we could write
something along the lines of:

```clojure
(def animal
  (projection/case-resolvable
    Zebra
    {:name              projection/leaf
     :number-of-stripes projection/leaf}
    Dolphin
    {:name         projection/leaf
     :intelligence projection/leaf}
    :else
    {:name projection/leaf}))
```

> __Note:__ Multiple options to dispatch on can be given by supplying a vector
> (e.g. `[Tiger Zebra]`) instead of just a single class.

Application is done as usual, retrieving different fields for different animals:

```clojure
(-> [(->Tiger 1) (->Dolphin 2) (->Zebra 5)]
    (projection/apply [animal])
    (engine/run!!))
;; => [{:name "Tiger"}
;;     {:name "Dolphin", :intelligence 80}
;;     {:name "Zebra", :number-of-stripes 20}]
```

Similarly, you can use [[case]] to dispatch on the class of the result, i.e.
_after resolution_.

### Dispatch on Partial Result

Let's keep our animals, but now let's assume that they are not represented by
different resolvables classes but can be identified using a `:type` key within
the result:

```clojure
(defrecord Animal [id]
  data/Resolvable
  (resolve! [_ _]
    (d/future
      (case (mod id 3)
        0 {:type :tiger, :name "Tiger", :number-of-stripes (* id 4)}
        1 {:type :zebra, :name "Zebra", :number-of-stripes (* id 4)}
        2 {:type :dolphin, :name "Dolphin", :intelligence (* id 40)}))))
```

To handle this we have to retrieve the `:type` key first and decide on what
projection to _actually_ use based on its value. Enter the [[conditional]]
projection:

```clojure
(def animal
  (projection/conditional
    {:type projection/leaf}
    (comp #{:zebra} :type) {:name projection/leaf, :number-of-stripes projection/leaf}
    (comp #{:dolphin} :type) {:name projection/leaf, :intelligence projection/leaf}
    :else {:name projection/leaf}))
```

What happens here is that first we project any given element using `{:type
projection/leaf}` whose result will then be used to find a matching predicate.
The corresponding projection is then re-applied to the initial element.

```clojure
(-> [(->Animal 1) (->Animal 2) (->Animal 3)]
    (projection/apply [animal])
    (engine/run!!))
;; => [{:name "Zebra", :number-of-stripes 4}
;;     {:name "Dolphin", :intelligence 80}
;;     {:name "Tiger"}]
```

> __Note:__ Don't forget the vector around the `(projection/conditional ...)`
> form – after all we want to apply it to each element.

### Arbitrary Transformation

If you need to change the structure of your data (e.g. extracting keys, merging
subtrees, ...) you can use [[transform]].

```clojure
(def sum-counts
  (projection/transform
    #(apply + (map :count %))
    [{:count projection/leaf}]))
```

Optionally, you can supply an output template, that will be applied to the
transformed value:

```clojure
(def sum-counts
  (projection/transform
    #(apply + (map :count %))
    [{:count projection/leaf}]
    projection/leaf))
```

As expected, this takes a seq of maps with at least the `:count` key and
produces a single leaf value:

```clojure
(-> [{:type :zebra, :count 10}, {:type :dolphin, :count 5}]
    (projection/apply sum-counts)
    (engine/run!!))
;; => 15
```

### Dependent Projections

The above transformation and dispatch mechanisms could be seen – in one way or
another – as special cases of a more generic approach:

1. Use an initial projection to generate a partial result.
2. Use the partial result to generate a new projection.
3. Apply the new projection to the initial value.

Consider the following example where each `Person` has a list of followers,
again `Person` values.

```clojure
(declare ->Person)

(defrecord Person [id]
  data/Resolvable
  (resolve! [_ _]
    (d/future
      {:id id, :followers (map ->Person (range (inc id) (+ id 15) 3))})))
```

A valid question here could be: "Does the person in question follow their
followers back?" Let's answer it by firstly specifying what "X follows Y" means
– which is clearly that their IDs have the same last digit:

```clojure
(defrecord IsFollowing [person-id follower-id]
  data/Resolvable
  (resolve! [_ _]
    (= (mod person-id 10) (mod follower-id 10))))
```

Now, we can adjust any `Person` projection to inject an `IsFollowing` record
into the person map, describing if a given `person-id` is following them.

```clojure
(defn add-followed-by
  [template k person-id]
  (projection/let [{:keys [id]} {:id projection/leaf}]
    (projection/union
      {k (projection/value (->IsFollowing id person-id))}
      template)))
```

> __Remember:__ [[value]] can be used to inject/override subtrees.

All that remains is to remember the ID of the top-level `Person` and use it to
generate our concrete `IsFollowing` injection:

```clojure
(def person-with-followers
  (projection/let [{:keys [id]} {:id projection/leaf}]
    {:id projection/leaf
     :followers [(-> {:id projection/leaf}
                     (add-followed-by :followed-by-parent? id))]}))
```

And the projected result will finally answer our question:

```clojure
(-> (->Person 1)
    (projection/apply person-with-followers)
    (engine/run!!))
;; => {:id 1
;;     :followers ({:id 2,  :followed-by-parent? false}
;;                 {:id 5,  :followed-by-parent? false}
;;                 {:id 8,  :followed-by-parent? false}
;;                 {:id 11, :followed-by-parent? true}
;;                 {:id 14, :followed-by-parent? false})}
```

> __Note:__ In this case one might also think about offering `:followed-by?` as
> a `Person` property and using [[parameters]] to inject the top-level
> `person-id` into each follower.
