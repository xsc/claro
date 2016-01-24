(ns claro.engine.middlewares.trace
  (:require [claro.engine.protocols :as engine]
            [claro.runtime.impl :as impl]))

;; ## Helper

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

;; ## Middleware

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
       (engine/wrap-resolver engine)))
