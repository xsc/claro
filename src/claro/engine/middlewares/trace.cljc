(ns claro.engine.middlewares.trace
  (:require [claro.engine.protocols :as engine]
            [claro.runtime.impl :as impl]))

;; ## Helper

(defn- trace!
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

(defn tracing
  "Wrap the given Engine to produce trace output after each resoltion."
  [engine]
  (->> (fn [resolver]
         (fn [batch]
           (let [start (System/nanoTime)]
             (try
               (impl/chain
                 (engine/impl engine)
                 (resolver batch)
                 #(trace! batch start %))
               (catch Throwable t
                 (trace! batch start t))))))
       (engine/wrap-resolver engine)))
