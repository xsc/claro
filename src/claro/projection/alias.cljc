(ns claro.projection.alias
  (:refer-clojure :exclude [alias])
  (:require [claro.projection.protocols :as pr]
            [claro.data.ops.then :refer [then]]))

;; ## Record

(defn- assert-map!
  [value this]
  (when-not (map? value)
    (throw
      (IllegalArgumentException.
        (format "'alias' projection %s expects a map but value is not: %s"
                (pr-str this)
                (pr-str value)))))
  value)

(defn- assert-contains!
  [value k this]
  (when-not (contains? value k)
    (throw
      (IllegalArgumentException.
        (str "'alias' projection " (pr-str this)
             " expects key '" k "' but value "
             "does not contain it: " (pr-str value)))))
  value)

(defn- assert-no-override!
  [value k this]
  (when (contains? value k)
    (throw
      (IllegalArgumentException.
        (str "'alias' projection " (pr-str this)
             " would override key '" k "' in: "
             (pr-str value)))))
  value)

(defn- alias-value
  [value alias-key key template]
  {alias-key (pr/project-template template (get value key))})

(defrecord Alias [alias-key key template]
  pr/Projection
  (project-template [this value]
    (->> (fn [value]
           (-> value
               (assert-map! this)
               (assert-contains! key this)
               (assert-no-override! alias-key this)
               (alias-value alias-key key template)))
         (then value))))

;; ## Constructor

(defn alias
  "Extract the value at the given key to `alias-key`, using `template`
   for further projection. This will generate a map with a single key,
   so to retain additional information you'll need to employ `union`:

   ```clojure
   (union
     [(alias :person-with-name :person-a {:name leaf})
      (alias :person-with-id   :person-b {:id leaf})
      {:another-key leaf}])
   ```

   This projection has to be applied to a map."
  [alias-key key template]
  {:pre [(not= alias-key key)]}
  (->Alias alias-key key template))
