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

(def gen-int-transform
  (->> (gen/tuple
         (gen/elements [+ - * /])
         (gen/such-that (complement zero?) gen/int))
       (gen/fmap
         (fn [[f v]]
           #(f % v)))))

(def gen-infinite-seq-transform
  (gen/fmap
    (fn [f]
      #(update % :n f))
    gen-int-transform))

(def gen-transformation
  (gen/let [{:keys [depth template] :as data} (g/raw-template :valid)]
    (gen/let [transform-depth (gen/large-integer* {:min 1, :max depth})
              transform-fn    gen-infinite-seq-transform]
      (let [path (repeat (dec transform-depth) :next)]
        (gen/return
          (-> data
              (merge
                {:raw-template     template
                 :partial-template (get-in template path)
                 :path             path
                 :transform-fn     transform-fn
                 :transform-depth  transform-depth})
              (update-in
                (cons :template path)
                #(projection/transform transform-fn %))))))))

;; ## Tests

(defspec t-transform (test/times 100)
  ;; If we transform the nth level of a template, a projected infinite seq
  ;; should be:
  ;; - identical to one projected with the original template up to level (n-1),
  ;; - identical to an infinite seq where the transformation and the corres-
  ;;   ponding partial template was applied for the remaining levels.
  (let [run! (make-engine)]
    (prop/for-all
      [value (g/infinite-seq)
       data  gen-transformation]
      (let [{:keys [template raw-template partial-template
                    path transform-depth transform-fn]} data
            raw-result @(-> value (projection/apply raw-template) (run!))
            partial-result @(-> value
                                (update :n + transform-depth -1)
                                (transform-fn)
                                (projection/apply partial-template)
                                (run!))]
        (= (if (seq path)
             (assoc-in raw-result path partial-result)
             partial-result)
           @(-> value (projection/apply template) (run!)))))))

(defspec t-transform-seq (test/times 100)
  (let [run! (make-engine)]
    (prop/for-all
      [values       (gen/vector (g/infinite-seq))
       transform-fn gen-infinite-seq-transform
       template     (g/valid-template)]
      (= @(-> (mapv transform-fn values)
              (projection/apply [template])
              (run!))
         @(-> values
              (projection/apply [(projection/transform transform-fn template)])
              (run!))))))

(defspec t-transform-at (test/times 100)
  (let [run! (make-engine)]
    (prop/for-all
      [value        (g/infinite-seq)
       template     (g/valid-template)
       transform-fn gen-infinite-seq-transform]
      (= {:nested @(-> (transform-fn value)
                       (projection/apply template)
                       (run!))}
         @(-> {:nested value}
              (projection/apply
                (projection/transform-at :nested transform-fn template))
              (run!))))))

(defspec t-transform-at-with-alias (test/times 100)
  (let [run! (make-engine)]
    (prop/for-all
      [value        (g/infinite-seq)
       template     (g/valid-template)
       transform-fn gen-infinite-seq-transform]
      (= {:alias @(-> (transform-fn value)
                      (projection/apply template)
                      (run!))}
         @(-> {:nested value}
              (projection/apply
                (projection/transform-at :nested transform-fn template :alias))
              (run!))))))

(defspec t-transform-at-with-alias-and-union (test/times 100)
  (let [run! (make-engine)]
    (prop/for-all
      [value        (g/infinite-seq)
       template     (g/valid-template)
       transform-fn gen-infinite-seq-transform]
      (let [union-template
            {:next {:value projection/leaf}}
            transform-template
            (projection/transform-at :nested transform-fn template :alias)]
        (= {:nested @(-> value
                         (projection/apply union-template)
                         (run!))
            :alias @(-> (transform-fn value)
                        (projection/apply template)
                        (run!))}
           @(-> {:nested value}
                (projection/apply
                  (projection/union
                    [{:nested union-template}
                     transform-template]))
                (run!)))))))
