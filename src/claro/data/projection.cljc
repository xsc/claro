(ns claro.data.projection
  (:require [claro.data.composition :as composition]
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
      (composition/chain-when
        value
        #(and (coll? %) (not (map? %)))
        (fn [result-sq]
          (into (empty result-sq)
                (map #(project* template %))
                result-sq)))
      (composition/chain-select-keys value sq))))

;; ### Map

(defn- project-keys*
  [value templates]
  (->> (for [[k template] templates]
         [k #(project* template %)])
       (into {})
       (composition/chain-keys value)))

(extend-protocol ProjectionTemplate
  clojure.lang.IPersistentMap
  (project* [templates value]
    (let [ks (keys templates)]
      (-> value
          (project-keys* templates)
          (composition/chain-select-keys ks)))))

;; ### Values

(extend-protocol ProjectionTemplate
  nil
  (project* [_ value]
    value))
