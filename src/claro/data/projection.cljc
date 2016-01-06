(ns claro.data.projection
  (:require [claro.data.ops :as ops]
            [claro.data.tree :as tree]
            [potemkin :refer [defprotocol+]]))

;; ## Protocol

(defprotocol+ ProjectionTemplate
  (project* [template value]))

(defn project
  [value template]
  (project* template value))

;; ### Sequences

(extend-protocol ProjectionTemplate
  clojure.lang.Sequential
  (project* [[template :as sq] value]
    {:pre [(= (count sq) 1)]}
    (if (and (= (count sq) 1) (satisfies? ProjectionTemplate template))
      (tree/chain-when
        value
        #(and (coll? %) (not (map? %)))
        (fn [result-sq]
          (into (empty result-sq)
                (map #(project* template %))
                result-sq)))
      (ops/select-keys value sq))))

;; ### Map

(defn- project-keys*
  [value templates]
  (->> (for [[k template] templates]
         [k #(project* template %)])
       (into {})
       (ops/update-keys value)))

(extend-protocol ProjectionTemplate
  clojure.lang.IPersistentMap
  (project* [templates value]
    (let [ks (keys templates)]
      (-> value
          (project-keys* templates)
          (ops/select-keys ks)))))

;; ### Values

(extend-protocol ProjectionTemplate
  nil
  (project* [_ value]
    value))
