(ns claro.middleware.intercept-test
  (:require [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [clojure.test :refer :all]
            [claro.test :as test]
            [claro.data :as data]
            [claro.engine :as engine]
            [claro.middleware.intercept :refer :all]))

;; ## Resolvables

(defrecord Value [value intercept-value]
  data/Resolvable
  (resolve! [_ _]
    value))

(defrecord PureValue [value intercept-value]
  data/PureResolvable
  data/Resolvable
  (resolve! [_ _]
    value))

;; ## Helper

(defn run-intercept
  [value]
  (let [run! (-> (engine/engine {:check-cost? false})
                 (wrap-intercept
                   (fn [_ batch]
                     (->> (for [{:keys [value intercept-value] :as r} batch
                                :when intercept-value]
                            [r intercept-value])
                          (into {})))))]
    @(run! value)))

;; ## Tests

(defspec t-wrap-intercept (test/times 100)
  (prop/for-all
    [values (->> (gen/tuple gen/int (gen/one-of [gen/int (gen/return nil)]))
                 (gen/fmap #(apply ->Value %))
                 (gen/vector))]
    (= (map
         (fn [{:keys [intercept-value value]}]
           (or intercept-value value))
         values)
       (run-intercept values))))

(defspec t-wrap-intercept-ignores-pure-resolvables (test/times 100)
  (prop/for-all
    [values (->> (gen/tuple gen/int (gen/one-of [gen/int (gen/return nil)]))
                 (gen/fmap #(apply ->PureValue %))
                 (gen/vector))]
    (= (map :value values)
       (run-intercept values))))
