(ns claro.projection.alias-test
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

(defspec t-alias (test/times 100)
  (let [run! (make-engine)]
    (prop/for-all
      [value        (g/infinite-seq)
       end-template (g/valid-template)
       key-to-alias (gen/elements [:next :value])
       alias-depth  gen/s-pos-int
       alias-key    gen/string-ascii]
      (let [path (vec (repeat alias-depth :next))
            end-template (if (= key-to-alias :next)
                           end-template
                           projection/leaf)
            raw-template (assoc-in {} (conj path key-to-alias) end-template)
            alias-template (->> (projection/alias
                                  alias-key
                                  key-to-alias
                                  end-template)
                                (assoc-in {} path))]
        (= (-> value
               (projection/apply alias-template)
               (run!)
               (deref)
               (get-in (conj path alias-key)))
           (-> value
               (projection/apply raw-template)
               (run!)
               (deref)
               (get-in (conj path key-to-alias))))))))

(defspec t-alias-type-mismatch (test/times 25)
  (let [run! (make-engine)]
    (prop/for-all
      [value        (g/infinite-seq)
       alias-key    gen/string-ascii]
      (let [template (projection/alias alias-key :value projection/leaf)]
        (boolean
          (is
            (thrown-with-msg?
              IllegalArgumentException
              #"is a map but value is not"
              @(-> [value]
                   (projection/apply template)
                   (run!)))))))))

(defspec t-alias-missing-key (test/times 25)
  (let [run! (make-engine)]
    (prop/for-all
      [value        (g/infinite-seq)
       alias-key    gen/string-ascii]
      (let [template (projection/alias alias-key :unknown projection/leaf)]
        (boolean
          (is
            (thrown-with-msg?
              IllegalArgumentException
              #"expects key"
              @(-> value
                   (projection/apply template)
                   (run!)))))))))
