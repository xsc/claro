(ns claro.data-test
  (:require [clojure.test.check :as tc]
            [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [clojure.test :refer :all]
            [claro.data :as data]))

;; ## Fixtures
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
                  [(class (first batch)) (count batch)])
           (f batch)))})))

;; ## Simple Resolution

(defrecord Apple [colour]
  data/Resolvable
  (resolve! [_ _]
    {:type :apple
     :colour colour}))

(defrecord AppleBasket [size]
  data/Resolvable
  (resolve! [_ {:keys [colours]}]
    (vec (take size (repeatedly #(Apple. (rand-nth colours)))))))

(defspec t-simple-resolution 100
  (prop/for-all
    [basket-size gen/pos-int]
    (let [resolutions (atom [])
          run! (make-engine resolutions {:env {:colours [:red :green]}})
          basket (run! (AppleBasket. basket-size))]
      (and (is (instance? clojure.lang.IDeref basket))
           (is (= basket-size (count @basket)))
           (is (every? #{:apple} (map :type @basket)))
           (is (every? #{:red :green} (map :colour @basket)))
           (is (= [AppleBasket 1] (first @resolutions)))
           (is (= (when (pos? basket-size)
                    [Apple (count (set (map :colour @basket)))])
                  (second @resolutions)))))))

;; ## Max Batch Limit

(defrecord Nested [n max-n]
  data/Resolvable
  (resolve! [_ _]
    (when (< n max-n)
      {:nested (Nested. (inc n) max-n)})))

(defspec t-max-batches 20
  (prop/for-all
    [max-n       gen/pos-int
     max-batches gen/s-pos-int]
    (let [resolutions (atom [])
          run! (make-engine resolutions {:max-batches max-batches})
          result (run! (Nested. 0 max-n))]
      (cond (= max-n 0)           (is (nil? @result))
            (< max-n max-batches) (is (map? @result))
            :else (boolean
                    (is (thrown-with-msg?
                          IllegalStateException
                          #"resolution has exceeded maximum batch count/depth"
                          @result)))))))
