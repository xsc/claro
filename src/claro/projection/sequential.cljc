(ns claro.projection.sequential
  (:require [claro.projection.protocols :refer [Projection project]]
            [claro.data.error :refer [with-error?]]
            [claro.data.ops.chain :as chain]))

;; ## Helpers

(defn- assert-sequential!
  [value template]
  (when-not (sequential? value)
    (throw
      (IllegalArgumentException.
        (str "projection template is sequential but value is not.\n"
             "template: [" (pr-str template) "]\n"
             "value:    " (pr-str value)))))
  value)

(defprotocol WrapSequential
  (wrap-sequential [this values]))

(extend-protocol WrapSequential
  clojure.lang.IPersistentVector
  (wrap-sequential [_ values]
    values)

  clojure.lang.IPersistentList
  (wrap-sequential [_ values]
    (chain/chain-blocking* values list*))

  clojure.lang.ISeq
  (wrap-sequential [_ values]
    (chain/chain-blocking* values list*))

  clojure.lang.IPersistentCollection
  (wrap-sequential [coll values]
    (let [prototype (empty coll)]
      (chain/chain-blocking* values #(into prototype %)))))

(defn- project-elements
  [template value]
  (with-error? value
    (assert-sequential! value template)
    (wrap-sequential value (mapv #(project template %) value))))

;; ## Implementation

(extend-protocol Projection
  clojure.lang.Sequential
  (project [[template :as sq] value]
    {:pre [(= (count sq) 1)]}
    (chain/chain-eager value #(project-elements template %))))
