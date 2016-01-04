(ns claro.simple-resolution-bench
  (:require [perforate.core :refer [defgoal defcase]]
            [claro.data :as data]
            [claro.engine :as engine]
            [claro.resolvables :as rs]))

(def run!! engine/run!!)

(defgoal simple-resolution
  "Resolution of simple (non-batched) Resolvables")

(defcase simple-resolution :synchronous-resolution
  []
  (run!! (rs/make-sync)))

(defcase simple-resolution :asynchronous-resolution
  []
  (run!! (rs/make-async)))

(defcase simple-resolution :batched-resolution
  []
  (run!! (rs/make-batched)))
