(ns claro.tree.protocols
  (:require [potemkin :refer [defprotocol+]]))

;; ## Protocols

(defprotocol+ Node
  "Protocol for claro tree nodes."
  (resolvables-into! [this sq]
    "Fill the given `java.util.Collection` with the current node's resolvables.")
  (unresolved? [this]
    "Return true if this is an unresolved node. Calling `fold` on resolved
     nodes should not have any effect.")
  (fpartial* [this fs]
    "Apply the given functions to the potentially partial representation of
     this node. They will only be called during [[fold]].")
  (fold [this resolvable->result]
    "Fold this node, replacing the given resolvables with the given result.")
  (unwrap [this not-possible]
    "Return the value contained within the node, or `not-possible` if it is not
     possible to compute such a value."))

(defprotocol+ PartialNode
  (unwrap-partial [this not-possible]
    "Like [[unwrap]] but will return `not-possible` if the value is
     not partially resolved."))

(defprotocol+ BlockingNode
  (fmap* [this fs]
    "Apply the given functions once this node is fully resolved. They will only
     be called during [[fold]]."))

(defprotocol+ TransformableNode
  (ftransform* [this f]
    "Transform the value within the node. This allows you to directly manipulate
     the resolvables exposed by a node."))

(defprotocol+ ErrorNode
  (error-message [this]
    "The error message contained in this node.")
  (error-data [this]
    "The error data cotnained in this node.")
  (error? [this]
    "Returns true if this node is an error node."))

(defprotocol+ Tree
  "Protocol for values that can be turned into [[Node]] values."
  (wrap [this]
    "Convert the given value to a [[Node]]."))

;; ## Helpers

(defmacro extend-defaults
  "Extend the given protocols for `Object` and `nil`."
  [& body]
  `(do
    (extend-type Object ~@body)
    (extend-type nil ~@body)))

(defn reduce-fns
  "Apply every function in `fs` to `value`. If an unresolved value is
   encountered, a tree node will be constructed using `rewrap-fn`."
  ([fs value]
   (reduce-fns fpartial* fs value))
  ([rewrap-fn fs value]
   (let [it (clojure.lang.RT/iter fs)]
     (loop [value value]
       (if (.hasNext it)
         (cond (error? value)
               value

               (unresolved? value)
               (rewrap-fn value (iterator-seq it))

               :else
               (let [f (.next it)]
                 (recur (f value))))
         value)))))

(defn reduce-fns-and-fold
  "Call [[reduce-fns]], then [[fold]] on the given value."
  ([fs value resolvable->result]
   (-> (reduce-fns fs value)
       (fold resolvable->result)))
  ([rewrap-fn fs value resolvable->result]
   (-> (reduce-fns rewrap-fn fs value)
       (fold resolvable->result))))
