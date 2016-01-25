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

(defn- gen-nested-map
  [leave-gen]
  (->> (gen/recursive-gen
         #(gen/map gen/string-ascii %)
         leave-gen)
       (gen/such-that map?)))

(defn- action-gen
  [k raw-op claro-op & gens]
  (->> (apply gen/tuple gens)
       (gen/fmap
         (fn [values]
           {:form (list* k '% values)
            :raw-action #(apply raw-op % values)
            :action #(apply claro-op % values)}))))

(let [k  gen/string-ascii
      ks (gen/vector k)
      v  (gen/return {})]
  (def ^:private gen-actions
    (gen/vector
      (gen/one-of
        [(action-gen 'assoc assoc data/assoc k v)
         (action-gen 'assoc-in assoc-in data/assoc-in ks v)
         (action-gen 'update update data/update k (gen/fmap constantly v))
         (action-gen 'update-in update-in data/update-in ks (gen/fmap constantly v))
         (action-gen 'select-keys select-keys data/select-keys ks)])
      1 20)))

;; ## Application

(defn- reduce-actions
  [initial-value k actions]
  (reduce #(%2 %1) initial-value (map k actions)))

;; ## Tests

(defrecord Identity [v]
  data/Resolvable
  (resolve! [_ _]
    v))

(defspec t-map-ops 100
  (prop/for-all
    [actions gen-actions
     unresolved-value (gen-nested-map (gen/elements [{} (->Identity {})]))]
    (let [run! (make-engine)
          resolved-value @(run! unresolved-value)
          value (reduce-actions unresolved-value :action actions)]
      (or (= value ::error)
          (let [expected (reduce-actions resolved-value :raw-action actions)
                result @(run! value)]
            (= result expected))))))
