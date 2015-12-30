(ns claro.resolvables
  (:require [claro.data :as data]))

;; ## Synchronous Resolution

(defrecord SyncLion [id]
  data/Resolvable
  (resolve! [_ _]
    (Thread/sleep 20)
    {:lion id}))

(defrecord SyncFlamingo [id]
  data/Resolvable
  (resolve! [_ _]
    (Thread/sleep 10)
    {:flamingo id}))

(defrecord SyncArea [count constructor]
  data/Resolvable
  (resolve! [_ _]
    (Thread/sleep (* count 5))
    {:animals (mapv #(constructor %) (range count))}))

(defrecord SyncSavannah [lion-count flamingo-count]
  data/Resolvable
  (resolve! [_ _]
    {:lions      (SyncArea. lion-count ->SyncLion)
     :flamingoes (SyncArea. flamingo-count ->SyncFlamingo)}))

(defn make-sync
  []
  (SyncSavannah. 3 5))

;; ## Asynchronous Resolution

(defrecord AsyncLion [id]
  data/Resolvable
  (resolve! [_ _]
    (future
      (Thread/sleep 20)
      {:lion id})))

(defrecord AsyncFlamingo [id]
  data/Resolvable
  (resolve! [_ _]
    (future
      (Thread/sleep 10)
      {:flamingo id})))

(defrecord AsyncArea [count constructor]
  data/Resolvable
  (resolve! [_ _]
    (future
      (Thread/sleep (* count 5))
      {:animals (mapv #(constructor %) (range count))})))

(defrecord AsyncSavannah [lion-count flamingo-count]
  data/Resolvable
  (resolve! [_ _]
    {:lions      (AsyncArea. lion-count ->AsyncLion)
     :flamingoes (AsyncArea. flamingo-count ->AsyncFlamingo)}))

(defn make-async
  []
  (AsyncSavannah. 3 5))

;; ## Batchwise Resolution

(defrecord BatchedLion [id]
  data/Resolvable
  data/BatchedResolvable
  (resolve-batch! [_ _ lions]
    (future
      (Thread/sleep 20)
      (mapv #(hash-map :lion (:id %)) lions))))

(defrecord BatchedFlamingo [id]
  data/Resolvable
  data/BatchedResolvable
  (resolve-batch! [_ _ flamingoes]
    (future
      (Thread/sleep 20)
      (mapv #(hash-map :flamingo (:id %)) flamingoes))))

(defrecord BatchedArea [count constructor]
  data/Resolvable
  data/BatchedResolvable
  (resolve-batch! [_ _ areas]
    (future
      (Thread/sleep (* (clojure.core/count areas) 5))
      (mapv
        (fn [{:keys [count constructor]}]
          {:animals (mapv #(constructor %) (range count))})
        areas))))

(defrecord BatchedSavannah [lion-count flamingo-count]
  data/Resolvable
  (resolve! [_ _]
    {:lions      (BatchedArea. lion-count ->BatchedLion)
     :flamingoes (BatchedArea. flamingo-count ->BatchedFlamingo)}))

(defn make-batched
  []
  (BatchedSavannah. 3 5))
