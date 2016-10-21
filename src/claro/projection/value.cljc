(ns claro.projection.value
  (:require [claro.projection.protocols :as pr]
            [claro.data.error :refer [with-error?]]
            [claro.projection.objects :refer [leaf]]))

;; ## Record

(defrecord ValueProjection [value template]
  pr/Projection
  (project [_ value']
    (with-error? value'
      (if template
        (pr/project template value)
        value))))

(defn ^:no-doc value?
  [v]
  (instance? ValueProjection v))

(defmethod print-method ValueProjection
  [^ValueProjection value ^java.io.Writer w]
  (let [t (.-template value)]
    (.write w "#<")
    (.write w (if t "value" "finite-value"))
    (.write w " ")
    (print-method (.-value value) w)
    (when t
      (.write w " -> ")
      (print-method (.-template value) w))
    (.write w ">")))

;; ## Constructor

(defn value
  "A projection that replaces any value it encounters with the given one.
   `template` will be used for further projection, if given, otherwise [[leaf]]
   is used.

   Note that this projection can be used to inject values into a map, i.e. the
   result of

   ```clojure
   {:id       projection/leaf
    :name     projection/leaf
    :visible? (projection/value true)}
   ```

   will always contain a key `:visible?` with value `true` – even if the
   original map had no such key."
  [value & [template]]
  (->ValueProjection value (or template leaf)))

(defn ^{:added "0.2.3"} finite-value
  "Like [[value]] but will not apply any further projection to the given value.
   This means that you can use this to inject arbitrary (but finite!) subtrees
   into your data.

   (If you still give a potentially infinite resolvable, you'll hit claro's
   resolution limits.)"
  [value]
  (->ValueProjection value nil))

(finite-value 1)
