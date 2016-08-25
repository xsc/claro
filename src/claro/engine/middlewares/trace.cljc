(ns claro.engine.middlewares.trace
  (:require [claro.engine.core :as engine]
            [claro.runtime.impl :as impl]))

;; ## General Stats

(defn- trace-stats!
  [batch start result]
  (let [delta (/ (- (System/nanoTime) start) 1e9)]
    (locking *out*
      (printf "[%s] " (.getName (class (first batch))))
      (if (instance? Throwable result)
        (print "an error occured")
        (print (count result) "of" (count batch) "elements resolved"))
      (printf " ... %.3fs%n" delta)
      (flush))
  (if (instance? Throwable result)
    (throw result)
    result)))

(defn trace-stats
  "Wrap the given Engine to produce trace output after each resolution."
  [engine]
  (->> (fn [resolver]
         (fn [env batch]
           (let [start (System/nanoTime)]
             (try
               (impl/chain
                 (engine/impl engine)
                 (resolver env batch)
                 #(trace-stats! batch start %))
               (catch Throwable t
                 (trace-stats! batch start t))))))
       (engine/wrap engine)))

;; ## Trace Results

(defn trace
  "Wrap the given Engine to trace actual inputs/ouputs of resolutions.
   `classes-to-trace` can be any collection of classes or a single one."
  [engine classes-to-trace]
  (let [trace? (comp
                 (if (coll? classes-to-trace)
                   (set classes-to-trace)
                   #{classes-to-trace})
                 class)
        impl (engine/impl engine)]
    (->> (fn [resolver]
           (fn [env [head :as batch]]
             (if (trace? head)
               (impl/chain
                 impl
                 (resolver env batch)
                 (fn [result]
                   (doseq [[in out] (map vector batch result)]
                     (locking *out*
                       (printf "! %s --> %s%n" (pr-str in) (pr-str out))))
                   (flush)
                   result))
               (resolver env batch))))
         (engine/wrap engine))))
