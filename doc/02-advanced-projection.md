# Advanced Projections

Sometimes, you need a bit more flexibility than the [basic projections][1] can
offer – and claro can provide that by allowing arbitrary, and even conditional,
transformations of your data.

[1]: 01-projection.md

### Dispatch on Class (`case`)

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

[[case]] dispatches on the `Resolvable` class, so we could write something along
the lines of:

```clojure
(-> [(->Tiger 1) (->Dolphin 2) (->Zebra 5)]
    (projection/apply
      [(projection/case
         Zebra
         {:name              projection/leaf
          :number-of-stripes projection/leaf}
         Dolphin
         {:name         projection/leaf
          :intelligence projection/leaf}
         :else
         {:name projection/leaf})])
    (engine/run!!))
;; => [{:name "Tiger"}
;;     {:name "Dolphin", :intelligence 80}
;;     {:name "Zebra", :number-of-stripes 20}]
```

> __Note:__ Multiple options to dispatch on can be given by supplying a vector
> (e.g. `[Tiger Zebra]`) instead of just a single class.

### Dispatch on Partial Result (`conditional`)

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
(-> [(->Animal 1) (->Animal 2) (->Animal 3)]
    (projection/apply
      [(projection/conditional
         {:type projection/leaf}
         (comp #{:zebra} :type) {:name projection/leaf, :number-of-stripes projection/leaf}
         (comp #{:dolphin} :type) {:name projection/leaf, :intelligence projection/leaf}
         :else {:name projection/leaf})])
    (engine/run!!))
;; => [{:name "Zebra", :number-of-stripes 4}
;;     {:name "Dolphin", :intelligence 80}
;;     {:name "Tiger"}]
```

> __Note:__ Don't forget the vector around the `(projection/conditional ...)`
> form – after all we want to apply it to each element.

What happens here is that first we project any given element using `{:type
projection/leaf}` whose result will then be used to find a matching predicate.
The corresponding projection is then re-applied to the initial element.
