(ns claro.projection.value
  (:require [claro.projection.protocols :as pr]
            [claro.data.error :refer [with-error?]]
            [claro.projection.objects :refer [leaf]]))

;; ## Record

(defrecord ValueProjection [value template]
  pr/Projection
  (project [_ value']
    (with-error? value'
      (pr/project template value))))

(defn ^:no-doc value?
  [v]
  (instance? ValueProjection v))

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
