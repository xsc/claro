(ns claro.engine-test
  (:require [clojure.test.check :as tc]
            [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [clojure.test :refer :all]
            [claro.data :as data]
            [claro.engine :as engine]))

;; ## Fixtures

(defn make-engine
  [resolutions & [more-opts]]
  (engine/wrap
    (engine/engine more-opts)
    (fn [f]
      (fn [batch]
        (swap! resolutions
               (fnil conj [])
               [(class (first batch)) (count batch)])
        (f batch)))))

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
    (let [run! (make-engine (atom []) {:max-batches max-batches})
          result (run! (Nested. 0 max-n))]
      (cond (= max-n 0)           (is (nil? @result))
            (< max-n max-batches) (is (map? @result))
            :else (boolean
                    (is (thrown-with-msg?
                          IllegalStateException
                          #"resolution has exceeded maximum batch count/depth"
                          @result)))))))

;; ## Collections

(defspec t-collection-type-maintained 100
  (let [apple-gen (->> [(Apple. :red) (Apple. :green)]
                       (gen/elements)
                       (gen/vector)
                       (gen/tuple (gen/elements [[] ()  #{}]))
                       (gen/fmap
                         (fn [[empty-coll apples]]
                           (into empty-coll apples))))]
    (prop/for-all
      [apples apple-gen]
      (let [run! (make-engine (atom []))
            result @(run! apples)]
        (and (is (= (count apples) (count result)))
             (is (= (class apples) (class result)))
             (is (every? #{:apple} (map :type result)))
             (is (every? #{:red :green} (map :colour result))))))))

;; ## Maps

(let [path-gen (->> gen/simple-type
                    (gen/such-that (complement #(and (number? %) (Double/isNaN %))))
                    (gen/vector)
                    (gen/not-empty))]

  (defspec t-map-value-resolution 20
    (prop/for-all
      [apple-path path-gen]
      (let [run! (make-engine (atom []))
            result @(run! (assoc-in {} apple-path (Apple. :red)))]
        (and (is (map? result))
             (is (= {:type :apple, :colour :red}
                    (get-in result apple-path)))))))

  (defspec t-map-key-resolution 20
    (prop/for-all
      [apple-path path-gen]
      (let [run! (make-engine (atom []))
            result @(run! (assoc-in {}
                                    apple-path
                                    {(Apple. :red) :red
                                     (Apple. :green) :green}))]
        (and (is (map? result))
             (let [m (get-in result apple-path)]
               (and (is (= 2 (count m)))
                    (is (= (vector
                             [{:type :apple, :colour :green} :green]
                             [{:type :apple, :colour :red} :red])
                           (sort-by val m))))))))))

;; ## Records

(defrecord Wrapper [value])

(deftest t-record-resolution
  (let [run! (make-engine (atom []))
        value (Wrapper. (Apple. :red))
        result @(run! value)]
    (is (instance? Wrapper result))
    (is (= {:type :apple, :colour :red} (:value result)))))
