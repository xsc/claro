(ns claro.data.transform
  (:require [claro.data.protocols :as p]))

(defmacro ^{:added "0.2.7"} extend-list-transform
  "Implement the [[Transform]] protocol for the given `resolvable-class`,
   assuming it returns a seq of elements to-be-processed with
   `element-constructor`.

   ```clojure
   (extend-list-transform
     PeopleByLocation
     [->Person])
   ```

   This is equivalent to:

   ```clojure
   (extend-protocol claro.data/Transform
     PeopleByLocation
     (transform [_ results]
       (mapv ->Person results)))
   ```

   It's also possible to supply extra parameters to be passed to the element
   constructor, e.g.:

   ```clojure
   (extend-list-transform
     PeopleByLocation
     [->Person {:by :location}])
   ```

   This will call `(->Person element {:by :location})` on each element."
  ([resolvable-class [element-constructor & args]]
   {:pre [resolvable-class element-constructor]}
   `(let [constructor# #(~element-constructor % ~@args)]
      (extend-protocol p/Transform
        ~resolvable-class
        (~'transform [_# elements#]
          (mapv constructor# elements#)))))
  ([resolvable-class element-constructor & more]
   `(do
      (extend-list-transform ~resolvable-class ~element-constructor)
      (extend-list-transform ~@more))))

(defmacro ^{:added "0.2.7"} extend-transform
  "Implement the [[Transform]] protocol for the given `resolvable-class` by
   transforming/renaming fields according to a given field spec.

   ```clojure
   (extend-transform
     Person
     {:pet      [->Pet :pet-id]
      :location (fn [{:keys [latitude longitude]}]
                  (->Location latitude longitude))
      :name     :username})
   ```

   This will:

   - create `:pet` as `(->Pet (:pet-id result))`,
   - create `:location` as the result of the given function, and
   - copy `:username` to `:name`.

   All these take the resolution result (!) as input but will not alter `nil`
   values."
  ([resolvable-class fields]
   {:pre [resolvable-class (map? fields)]}
   (letfn [(->fn [[field-key value]]
             (let [result (gensym "result")]
               (cond (vector? value)
                     (let [[f & fields] value]
                       `(fn [~result]
                          (assoc ~result
                                 ~field-key
                                 (~f ~@(map #(list % result) fields)))))

                     (keyword? value)
                     `(fn [~result]
                        (assoc ~result
                               ~field-key
                               (get ~result ~value)))

                     :else
                     `(let [f# ~value]
                        (fn [~result]
                          (assoc ~result
                                 ~field-key
                                 (f# ~result)))))))]
     `(let [transform# (comp ~@(map ->fn fields))]
        (extend-protocol p/Transform
          ~resolvable-class
          (~'transform [_# result#]
            (some-> result# transform#))))))
  ([resolvable-class fields & more]
   `(do
      (extend-transform ~resolvable-class ~fields)
      (extend-transform ~@more))))
