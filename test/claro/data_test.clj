(ns claro.data-test
  (:require [clojure.test.check :as tc]
            [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [clojure.test :refer :all]
            [claro.data :as data]))

;; ## Fixtures

(defrecord Apple [colour]
  data/Resolvable
  (resolve! [_ _]
    {:type :apple
     :colour colour}))

(defrecord AppleBasket [size]
  data/Resolvable
  (resolve! [_ {:keys [colours]}]
    (vec (take size (repeatedly #(Apple. (rand-nth colours)))))))

(defrecord Person [id]
  data/Resolvable
  (resolve! [_ _]
    {:id id
     :father (Person. (* id 3))
     :mother (Person. (* id 5))}))

(defn make-engine
  [resolutions & [more-opts]]
  (data/engine
    (merge
      more-opts
      {:wrap-resolve
       (fn [f]
         (fn [batch]
           (swap! resolutions
                  (fnil conj [])
                  [(class (first batch)) (count batch)] )
           (f batch)))})))

;; ## Tests

(defspec t-simple-resolution 100
  (prop/for-all
    [basket-size gen/pos-int]
    (let [resolutions (atom [])
          run! (make-engine resolutions {:env {:colours [:red :green]}})
          basket (run! (AppleBasket. basket-size))]
      (and (is (instance? clojure.lang.IDeref basket))
           (is (= basket-size (count @basket)))
           (is (every? #{:apple} (map :type @basket)))
           (is (every? #{:red :green} (map :colour @basket)))))))
