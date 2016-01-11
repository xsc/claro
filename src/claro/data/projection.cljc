(ns claro.data.projection
  (:require [claro.data.ops
             [collections :as c]
             [maps :as m]]
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
      (c/map #(project* template %) value)
      (m/select-keys value sq))))

;; ### Map

(extend-protocol ProjectionTemplate
  clojure.lang.IPersistentMap
  (project* [templates value]
    (let [ks (keys templates)]
      (-> (reduce
            (fn [value [k template]]
              (m/update value k #(project* template %)))
            value templates)
          (m/select-keys ks)))))

;; ### Values

(extend-protocol ProjectionTemplate
  nil
  (project* [_ value]
    value))
