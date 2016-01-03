(defproject claro "0.1.0-SNAPSHOT"
  :description "claro que sí"
  :url "https://github.com/xsc/claro"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"
            :year 2015
            :key "mit"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [potemkin "0.4.2"]
                 [manifold "0.1.1"]
                 [prismatic/schema "1.0.4"]]
  :profiles {:dev {:dependencies [[org.clojure/test.check "0.9.0"]]
                   :plugins [[perforate "0.3.4"]]
                   :source-paths ["benchmarks"]
                   :global-vars {*warn-on-reflection* true}
                   :perforate
                   {:environments
                    [{:name :resolution-benchmarks
                      :namespaces [claro.simple-resolution]}]}}}
  :pedantic? :abort)
