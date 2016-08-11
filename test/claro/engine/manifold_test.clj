(ns claro.engine.manifold-test
  (:require [clojure.test.check :as tc]
            [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [clojure.test :refer :all]
            [claro.test :as test]
            [claro.data :as data]
            [claro.engine.fixtures :refer [make-engine]]
            [manifold.deferred :as d])
  (:import [java.util.concurrent Callable Executors ExecutorService TimeUnit]))

;; ## Resolvables

(defrecord Single [resolver value]
  data/Resolvable
  (resolve! [_ _]
    (resolver value)))

(defrecord Batched [resolver value]
  data/Resolvable
  data/BatchedResolvable
  (resolve-batch! [_ _ bs]
    (resolver (mapv :value bs))))

;; ## Generators

(defn gen-resolver
  [^ExecutorService executor-service]
  (gen/elements
    {:d/future      #(d/future %)
     :d/future-with #(d/future-with executor-service %)
     :deferred      #(doto (d/deferred) (d/success! %))
     :deferred-with #(doto (d/deferred executor-service) (d/success! %))
     :submit        #(.submit executor-service ^Callable (constantly %))
     :future        #(future %)
     :delay         #(delay %)
     :promise       #(doto (promise) (deliver %))}))

(defn- gen-resolvable
  [executor-service]
  (gen/let [value gen/simple-type-printable
            [type resolver] (gen-resolver executor-service)]
    {:resolvable (->Single resolver value)
     :type type
     :expected value}))

(defn- gen-resolvables
  [executor-service]
  (gen/let [values (gen/vector gen/simple-type-printable 1 8)
            [type resolver] (gen-resolver executor-service)]
    {:resolvable (mapv #(->Single resolver %) values)
     :type type
     :expected values}))

(defn- gen-batched-resolvables
  [executor-service]
  (gen/let [values (gen/vector gen/simple-type-printable 1 8)
            [type resolver] (gen-resolver executor-service)]
    {:resolvable (mapv #(->Batched resolver %) values)
     :type type
     :expected values}))

;; ## Fixtures

(def ^:dynamic *executor-service* nil)

(use-fixtures
  :once
  (fn [f]
    (let [e (Executors/newSingleThreadExecutor)]
      (try
        (binding [*executor-service* e]
          (f))
        (finally
          (.shutdown e))))))

;; ## Tests

(defmacro resolution-prop
  [generator]
  `(prop/for-all
     [~'{:keys [resolvable expected]} (~generator *executor-service*)]
     (let [run!# (make-engine)
           ~'result @(run!# ~'resolvable)]
       (= ~'expected ~'result))))

(defspec t-manifold (test/times 100)
  (resolution-prop gen-resolvable))

(defspec t-mixed-manifold (test/times 100)
  (resolution-prop gen-resolvables))

(defspec t-batched-manifold (test/times 100)
  (resolution-prop gen-batched-resolvables))
