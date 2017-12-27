(ns claro.tree.inner.navigators
  (:require [com.rpl.specter :as specter]
            [claro.tree.protocols :as tree]))

;; ## Background
;;
;; We can write more optimised navigators due to our domain knowledge.
;; We don't need:
;; - removal of values
;; - an implementation of 'select*'

;; ## Maps

(specter/defnav MAP-VALS-UNRESOLVED
  []
  (select*
    [this structure next-fn]
    structure)
  (transform*
    [this structure next-fn]
    (persistent!
      (reduce-kv
        (fn [result k v]
          (if (tree/unresolved? v)
            (assoc! result k (next-fn v))
            result))
        (transient structure)
        structure))))

(specter/defnav MAP-KEYS-UNRESOLVED
  []
  (select*
    [this structure next-fn]
    structure)
  (transform*
    [this structure next-fn]
    (persistent!
      (reduce-kv
        (fn [result k v]
          (if (tree/unresolved? k)
            (-> result
                (dissoc! k)
                (assoc! (next-fn k) v))
            result))
        (transient structure)
        structure))))

;; ## Records

(specter/defnav RECORD-ALL
  []
  (select*
    [this structure next-fn]
    ;; no need to implement
    structure)
  (transform*
    [this structure next-fn]
    (reduce-kv
      (fn [result k v]
        (assoc result (next-fn k) (next-fn v)))
      structure
      structure)))

;; ## Vectors

(specter/defnav VECTOR-ALL
  []
  (select*
    [this structure next-fn]
    ;; no need to implement
    structure)
  (transform*
    [this structure next-fn]
    (let [it (clojure.lang.RT/iter structure)]
      (loop [result (transient [])]
        (if (.hasNext it)
          (recur (conj! result (next-fn (.next it))))
          (persistent! result))))))

;; ## Lists

(specter/defnav LIST-ALL
  []
  (select*
    [this structure next-fn]
    ;; no need to implement
    structure)
  (transform*
    [this structure next-fn]
    (let [it (clojure.lang.RT/iter structure)]
      (loop [result (transient [])]
        (if (.hasNext it)
          (recur (conj! result (next-fn (.next it))))
          (or (seq (persistent! result)) '()))))))
