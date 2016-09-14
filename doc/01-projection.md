# Projections

If you've read about claro `Resolvable` values you might have noticed that there
is nothing preventing you from creating infinite trees or cycles between
different resolvable types.

For example, a `Person` could have a list of friends. And why, I ask you, would
they themselves be represented as anything other than `Person` records?

```clojure
(declare ->FriendsOf)

(defrecord Person [id]
  data/Resolvable
  (resolve! [_ env]
    (d/future
      (-> (fetch-person! (:db env) id)
          (assoc :friends (->FriendsOf id))))))

(defrecord FriendsOf [id]
  data/Resolvable
  (resolve! [_ env]
    (d/future
      (->> (fetch-friend-ids! (:db env) id)
           (map ->Person)))))
```

Of course, this explodes horribly when resolving (after taking an equally
horrible amount of time, nonetheless):

```clojure
(engine/run!! (->Person 1))
;; => IllegalStateException: resolution has exceeded maximum batch size
```

But claro is built around infinite trees. It provides powerful facilites of
dealing with infinitely nested data – the most elegant of which are
_projections_.

```clojure
(require '[claro.projection :as projection])
```

### Overview

A projection describes how to convert an infinite tree into a finite one.
That's about it, really, so let's write our first projection:

```clojure
(def base-person
  {:id   projection/leaf
   :name projection/leaf})
```

This basically says: "Out of all the available fields, take `:id` and `:name`
and expect them to be leaves of the tree (i.e. not a collection)." Let's try it
out:

```clojure
(engine/run!!
  (projection/apply (->Person 1) base-person))
;; => {:id 1, :name "Sherlock Holmes"}
```

We threw away the list of `:friends` provided to us by `Person` since we never
mentioned it. Let's see who's in there by extending our base projection:

```clojure
(def person-with-friends
  {:id      projection/leaf
   :name    projection/leaf
   :friends [{:id   projection/leaf
              :name projection/leaf}]})

(engine/run!!
  (projection/apply (->Person 1) person-with-friends))
;; => {:id 1
;;     :name "Sherlock Holmes"
;;     :friends [{:id 2, :name "John Watson"}
;;               {:id 3, :name "Miss Hudson"}]}
```

As you can see, projections can be nested. And by putting them inside a
vector we apply them to every element of a seq.

### Interlude

__Projections force you to think about the shape of the data you want to
retrieve. They are query and schema at once.__

Most importantly, they move any transformation logic away from the actual data
access. This let's you naively create rich trees of entities – any subtree will
only be retrieved if someone asks for it.

And this leads to different views on the same entity being represented as
projections. You don't need the high-quality image URL on a list page? Remove it
from the projection. You need the new view counter to be displayed on all detail
pages? Well, just add it.

This doesn't mean you can just be careless, of course. Writing flexibly reusable
projections is just as much of a challenge as writing reusable code in general.

### More Projections

#### Union

Speaking of reusability, it can be useful to merge the results of multiple
projections:

```clojure
(def base-person
  {:id   projection/leaf
   :name projection/leaf})

(def friend-list
  {:friends [base-person]})

(def person-with-friends
  (projection/union
    [base-person
     friend-list]))
```

> __Note:__ You might be tempted to use `merge` in these cases. Don't, since it
> only works with plain-map projections and you might want to use others
> sometime in the future.

#### Parameterisation

Let's say it should be possible to influence how many of a person's friends are
returned. For this, we add a field to the respective record:

```clojure
(defrecord FriendsOf [id limit offset]
  data/Resolvable
  (resolve! [_ env]
    (d/future
      (->> (fetch-friend-ids! (:db env) id (or limit 10) (or offset 0))
           (map ->Person)))))
```

> __Note:__ If you have a list of entities somewhere, always make it possible to
> only return a subset (and ideally only return a limited number of items per
> default). Otherwise, long lists will undoubtedly bring your application to its
> knees.

We can now craft a special projection, only retrieving the names of the first
five friends of a given person:

```clojure
(def person-with-five-friends
  {:id      projection/leaf
   :name    projection/leaf
   :friends (projection/parameters {:limit 5} [{:name projection/leaf}])})
```

`parameters` takes two arguments: the parameters to inject, as well as _another_
projection that will be applied to the resulting subtree. See the documentation
of [[parameters]] for further details.

#### Aliases

If you want to apply multiple projections to the same subtree, you need to
uniquely name them (since otherwise, they'll just overwrite each other). This is
especially useful when injecting different parameters into the same field. Or
for simple renaming, e.g. from `:name` to `:person-name`:

```clojure
{:id                                   projection/leaf
 (projection/alias :person-name :name) projection/leaf}
```

For example, we could introduce a flag checking friend status to our `Person`
records:

```clojure
(defrecord Friend? [person-id friend-id]
  data/Resolvable
  (resolve! [_ env]
    ...))

(defrecord Person [id]
  data/Resolvable
  (resolve! [_ env]
    (d/future
      (merge
        (fetch-person! (:db env) id)
        {:friend-of? (->Friend? nil id)
         :friends    (->FriendsOf id)}))))
```

If we want to check whether a certain person is friends with two specific users
we can use [[alias]] and [[parameters]] to generate a result:

```clojure
(defn- friend-of?
  [alias-key person-id]
  {(projection/alias alias-key :friend-of?)
   (projection/parameters
     {:person-id person-id}
     {:name projection/leaf})))

(def person-with-certain-friends
  (projection/union
    [{:id projection/leaf
      :name projection/leaf}
     (friend-of? :friend-of-sherlock? 1)
     (friend-of? :friend-of-watson? 2)))
```

Applying this projection to a `Person` will produce a map akin to:

```clojure
{:id 3
 :name "Miss Hudson"
 :friend-of-sherlock? true
 :friend-of-watson? true}
```

#### Missing Values

Sometimes, fields can be `nil` which would cause any non-leaf projection to
panic. Using [[maybe]] we can handle this case, e.g. if we don't know whether a
`Person` actually exists:

```clojure
(projection/maybe {:name projection/leaf})
```

In the same vein, we might want to return a [[default]] value if we couldn't
retrieve a real one:

```clojure
(projection/default {:name projection/leaf} unknown-person)
```

This will apply the given projection either to any non-`nil` value or to
`unknown-person`.
