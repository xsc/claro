(ns claro.data.tree
  (:require [claro.data.resolvable :as r]
            [potemkin :refer [defprotocol+]]))

;; ## Protocols

(defprotocol+ TreeResolvable
  "A wrapper around a specific `Resolvable` within a specific tree."
  (resolvable [_]
    "Get the actual `Resolvable`.")
  (set-resolved-value [_ resolved-value]
    "Set the resolved value for the given `TreeResolvable`.")
  (resolve-in [_ tree]
    "Inject the given resolved value into the tree at the position(s), the given
     `TreeResolvable` is bound to."))

(defprotocol+ ResolvableTree
  "Protocol for trees of `Resolvables`."
  (resolved? [_]
    "Is this tree fully resolved?")
  (tree-resolvables [_]
    "Get all `TreeResolvables` from the given tree."))

(defprotocol+ WrappedTree
  (wrapped? [_]))

;; ## Implementations

;; ### Wrappers

(extend-protocol WrappedTree
  Object
  (wrapped? [_] false)
  nil
  (wrapped? [_] false))

;; ## Resolver

(defn resolve-all
  [value tree-resolvables]
  (loop [value       value
         sq          tree-resolvables
         resolvables []]
    (if-let [[r & rst] (seq sq)]
      (let [{value' :value, resolvables' :resolvables} (resolve-in r value)]
        (recur value' rst (into resolvables resolvables')))
      {:value (if value
                (if (and (resolved? value) (instance? clojure.lang.IMeta value))
                  (with-meta value {::resolved? true})
                  (vary-meta value dissoc ::resolved?)))
       :resolvables resolvables})))

;; ### Helpers

(defmacro with-resolved-check
  [value & body]
  `(let [v# ~value]
     (and (not  (r/resolvable? v#))
          (let [r# (get (meta v#) ::resolved? ::none)]
            (if (not= r# ::resolved?)
              r#
              (do ~@body))))))

;; ### Leaves

(defrecord Leaf [value resolved-value]
  TreeResolvable
  (resolvable [_]
    value)
  (set-resolved-value [this resolved-value]
    (assoc this :resolved-value resolved-value))
  (resolve-in [_ tree]
    {:value       resolved-value
     :resolvables (tree-resolvables resolved-value)}))

(extend-protocol ResolvableTree
  nil
  (resolved? [_]
    true)
  (tree-resolvables [_]
    nil)

  Object
  (resolved? [this]
    (not (r/resolvable? this)))
  (tree-resolvables [this]
    (if (r/resolvable? this)
      [(->Leaf this nil)]
      nil)))

;; ### Collections

(defrecord MapValueResolvable [tree-resolvable k]
  TreeResolvable
  (resolvable [_]
    (resolvable tree-resolvable))
  (set-resolved-value [this resolved-value]
    (assoc this
           :tree-resolvable
           (set-resolved-value tree-resolvable resolved-value)))
  (resolve-in [this tree]
    (let [v (get tree k)
          {:keys [value resolvables]} (resolve-in tree-resolvable v)]
      {:value
       (assoc tree k value),
       :resolvables
       (map #(assoc this :tree-resolvable %) resolvables)})))

(defrecord SequentialResolvable [tree-resolvable index]
  TreeResolvable
  (resolvable [_]
    (resolvable tree-resolvable))
  (set-resolved-value [this resolved-value]
    (assoc this
           :tree-resolvable
           (set-resolved-value tree-resolvable resolved-value)))
  (resolve-in [this tree]
    (let [postproc (if (list? tree) reverse identity)]
      (loop [new-value (empty tree)
             tree      tree
             i         0]
        (cond (empty? tree)
              (throw (IllegalStateException.))

              (= i index)
              (let [v (first tree)
                    {:keys [value resolvables]} (resolve-in tree-resolvable v)]
                {:value
                 (postproc (into (conj new-value value) (next tree)))
                 :resolvables
                 (map #(assoc this :tree-resolvable %) resolvables)})

              :else
              (recur (conj new-value (first tree)) (next tree) (inc i)))))))

(extend-protocol ResolvableTree
  clojure.lang.IPersistentCollection
  (resolved? [this]
    (with-resolved-check this
      (if (map? this)
        (every? resolved? (vals this))
        (every? resolved? this))))
  (tree-resolvables [this]
    (cond (r/resolvable? this)
          [(->Leaf this nil)]

          (map? this)
          (for [[k v] this
                tree-resolvable (tree-resolvables v)]
            (MapValueResolvable. tree-resolvable k))

          (sequential? this)
          (->> (map-indexed
                 (fn [index e]
                   (->> (tree-resolvables e)
                        (map #(SequentialResolvable. % index))))
                 (vec this))
               (reduce into []))

          :else nil)))
