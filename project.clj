(defproject claro "0.1.1"
  :description "claro que sí"
  :url "https://github.com/xsc/claro"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"
            :year 2015
            :key "mit"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [potemkin "0.4.3"]
                 [manifold "0.1.2"]
                 [prismatic/schema "1.0.5"]]
  :profiles {:dev {:dependencies [[org.clojure/test.check "0.9.0"]
                                  [org.clojure/core.async "0.2.374"]]
                   :plugins [[perforate "0.3.4"]]
                   :source-paths ["benchmarks"]
                   :perforate
                   {:environments
                    [{:name :resolution-benchmarks
                      :namespaces [claro.expansion-bench
                                   claro.projection-bench
                                   claro.simple-resolution-bench]}]}}}
  :pedantic? :abort)
