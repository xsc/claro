(ns claro.data.error-test
  (:require [clojure.test.check
             [properties :as prop]
             [generators :as gen]
             [clojure-test :refer [defspec]]]
            [claro.data.error :refer :all]))

;; ## Operations

(def err
  #(error (str %2) {:current-value %1}))

(def operations
  {`+   +
   `-   -
   `*   *
   `err err})

(def gen-operation
  (gen/let [op  (gen/elements (keys operations))
            arg gen/int]
    (list op arg)))

(def gen-operations
  (gen/vector gen-operation))

(defn expected-result
  [ops call-fn value]
  (if-let [[[op arg] & rst] (seq ops)]
    (let [f (get operations op)
          result (call-fn f value arg)]
      (if (error? result)
        result
        (recur rst call-fn result)))
    value))

(defn equals?
  [v1 v2]
  (if (and (error? v1) (error? v2))
    (and (= (error-message v1) (error-message v2))
         (= (error-data v1) (error-data v2)))
    (= v1 v2)))

;; ## Tests

(defspec t-unless-error-> 100
  (prop/for-all
    [initial-value gen/int
     operations    gen-operations]
    (equals?
      (expected-result operations #(%1 %2 %3) initial-value)
      (eval `(unless-error-> ~initial-value ~@operations)))))

(defspec t-unless-error->> 100
  (prop/for-all
    [initial-value gen/int
     operations    gen-operations]
    (equals?
      (expected-result operations #(%1 %3 %2) initial-value)
      (eval `(unless-error->> ~initial-value ~@operations)))))
