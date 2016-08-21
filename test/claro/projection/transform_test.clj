(ns claro.projection.transform-test
  (:require [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [clojure.test :refer :all]
            [claro.test :as test]
            [claro.projection.generators :as g]
            [claro.engine.fixtures :refer [make-engine]]
            [claro.data.ops.then :refer [then]]
            [claro.projection :as projection]))

;; ## Generators

(def gen-int-op
  (->> (gen/tuple
         (gen/elements [+ - * /])
         (gen/such-that (complement zero?) gen/int))
       (gen/fmap
         (fn [[f v]]
           #(f % v)))))

(def gen-infinite-seq-prepare
  (gen/fmap
    (fn [f]
      #(update % :n f))
    gen-int-op))

(def gen-preparation
  (gen/let [{:keys [depth template] :as data} (g/raw-template :valid)]
    (gen/let [depth (gen/large-integer* {:min 1, :max depth})
              op    gen-infinite-seq-prepare]
      (let [path (repeat (dec depth) :next)]
        (gen/return
          (-> data
              (merge
                {:raw-template     template
                 :partial-template (get-in template path)
                 :path             path
                 :op               op
                 :depth            depth})
              (update-in
                (cons :template path)
                #(projection/prepare op %))))))))

;; ## Tests

(defspec t-prepare (test/times 100)
  ;; If we alter the nth level of a template, a projected infinite seq
  ;; should be:
  ;; - identical to one projected with the original template up to level (n-1),
  ;; - identical to an infinite seq where the transformation and the corres-
  ;;   ponding partial template was applied for the remaining levels.
  (let [run! (make-engine)]
    (prop/for-all
      [value (g/infinite-seq)
       data  gen-preparation]
      (let [{:keys [template raw-template partial-template path depth op]} data
            raw-result @(-> value (projection/apply raw-template) (run!))
            partial-result @(-> value
                                (update :n + depth -1)
                                (op)
                                (projection/apply partial-template)
                                (run!))]
        (= (if (seq path)
             (assoc-in raw-result path partial-result)
             partial-result)
           @(-> value (projection/apply template) (run!)))))))

(defspec t-prepare-seq (test/times 100)
  (let [run! (make-engine)]
    (prop/for-all
      [values   (gen/vector (g/infinite-seq))
       op       gen-infinite-seq-prepare
       template (g/valid-template)]
      (= @(-> (mapv op values)
              (projection/apply [template])
              (run!))
         @(-> values
              (projection/apply [(projection/prepare op template)])
              (run!))))))

(defspec t-prepare-set (test/times 100)
  (let [run! (make-engine)]
    (prop/for-all
      [values   (gen/vector (g/infinite-seq))
       op       gen-infinite-seq-prepare
       template (g/valid-template)]
      (= @(-> (mapv op values)
              (projection/apply #{template})
              (run!))
         @(-> values
              (projection/apply #{(projection/prepare op template)})
              (run!))))))

(defspec t-prepare-with-alias (test/times 100)
  (let [run! (make-engine)]
    (prop/for-all
      [value    (g/infinite-seq)
       template (g/valid-template)
       op       gen-infinite-seq-prepare]
      (let [base-template
            {:next {:value projection/leaf}}
            transform-template
            (->> (projection/prepare op template)
                 (projection/alias :alias :nested))
            union-template
            (projection/union
              [{:nested base-template}
               transform-template])]
        (= {:nested @(-> value
                         (projection/apply base-template)
                         (run!))
            :alias @(-> (op value)
                        (projection/apply template)
                        (run!))}
           @(-> {:nested value}
                (projection/apply union-template)
                (run!)))))))
