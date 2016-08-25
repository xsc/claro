(ns claro.queries)

(def ^:private sleep
  (if-let [ms (some-> (System/getenv "CLARO_BENCHMARK_LATENCY") (Long.))]
    #(Thread/sleep ms)
    (constantly nil)))

(defn fetch-person!
  [id]
  (sleep)
  {:id        id
   :name      (str "Person #" id)
   :image-id   (* id 300)
   :friend-ids (range (inc id) (* id 10) (* 3 id))})

(defn fetch-image!
  [image-id]
  (sleep)
  (str "http://images.claro.de/" image-id ".png"))

(defn fetch-images!
  [image-ids]
  (sleep)
  (map
    #(str "http://images.claro.de/" % ".png")
    image-ids))

(defn fetch-people!
  [person-ids]
  (sleep)
  (map
    (fn [id]
      {:id        id
       :name      (str "Person #" id)
       :image-id   (* id 300)
       :friend-ids (range (inc id) (* id 10) (* 3 id))})
    person-ids))
