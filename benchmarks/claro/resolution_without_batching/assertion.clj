(ns claro.resolution-without-batching.assertion
  (:require [claro.resolution-without-batching
             [muse :as muse]
             [urania :as urania]
             [claro :as claro]]))

(let [fs {:muse muse/fetch-with-muse!
          :urania urania/fetch-with-urania!
          :claro claro/fetch-with-claro!
          :claro-and-projection claro/fetch-with-claro-and-projection!}
      results (->> (map (juxt key #((val %) 1)) fs)
                   (partition-by second)
                   (map #(map first %)))]
  (assert
    (= (count results) 1)
    (str "resolution returned different results. groups: "
         (pr-str results))))
