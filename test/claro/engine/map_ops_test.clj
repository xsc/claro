(ns claro.engine.map-ops-test
  (:require [clojure.test.check :as tc]
            [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [clojure.test :refer :all]
            [claro.data :as data]
            [claro.engine.fixtures :refer [make-engine]]))

;; ## Generator

(defn- action-gen
  [k raw-op claro-op & gens]
  (->> (apply gen/tuple gens)
       (gen/fmap
         (fn [values]
           {:form (list* k '% values)
            :raw-action #(apply raw-op % values)
            :action #(apply claro-op % values)}))))

(let [v gen/string-ascii
      vs (gen/vector v)]
  (def ^:private gen-actions
    (gen/vector
      (gen/one-of
        [(action-gen 'assoc assoc data/assoc v v)
         (action-gen 'assoc-in assoc-in data/assoc-in vs v)
         (action-gen 'update data/update v (gen/fmap constantly v))
         (action-gen 'update-in data/update-in vs (gen/fmap constantly v))
         (action-gen 'select-keys data/select-keys vs)])
      1 20)))

(defrecord Identity [v]
  data/Resolvable
  (resolve! [_ _]
    v))

(def ^:private gen-nested-map
  (->> (gen/recursive-gen
         #(gen/map gen/string-ascii %)
         (gen/one-of
           [gen/string-ascii
            (gen/fmap ->Identity gen/string-ascii)]))
       (gen/such-that map?)))

;; ## Application

(defmacro catch-it
  [form]
  `(try
     (do ~form)
     (catch ClassCastException t#
       ::error)
     (catch AssertionError t#
       ::error)))

(defn- reduce-actions
  [initial-value k actions]
  (reduce #(%2 %1) initial-value (map k actions)))

;; ## Tests

(defspec t-map-ops 100
  (prop/for-all
    [actions gen-actions
     unresolved-value gen-nested-map]
    (let [run! (make-engine)
          resolved-value @(run! unresolved-value)
          value (catch-it (reduce-actions unresolved-value :action actions))]
      (or (= value ::error)
          (let [expected (catch-it (reduce-actions resolved-value :raw-action actions))
                result (catch-it @(run! value))]
            (= result expected))))))
