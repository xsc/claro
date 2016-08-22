(defproject claro "0.2.0-SNAPSHOT"
  :description "claro que sí"
  :url "https://github.com/xsc/claro"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"
            :year 2015
            :key "mit"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [potemkin "0.4.3"]
                 [manifold "0.1.5"]]
  :profiles {:dev
             {:dependencies [[org.clojure/test.check "0.9.0"]
                             [org.clojure/core.async "0.2.385"]
                             [com.gfredericks/test.chuck "0.2.7"]]
              :plugins [[perforate "0.3.4"]]
              :source-paths ["benchmarks"]
              :perforate
              {:environments
               [{:name :resolution-benchmarks
                 :namespaces [claro.expansion-bench
                              claro.projection-bench
                              claro.simple-resolution-bench]}]}}
             :codox
             {:dependencies [[org.clojure/tools.reader "1.0.0-beta2"]]
              :plugins [[lein-codox "0.9.6"]]
              :codox {:project {:name "claro"}
                      :metadata {:doc/format :markdown}
                      :source-paths ["src"]
                      :source-uri "https://github.com/xsc/claro/blob/master/{filepath}#L{line}"
                      :namespaces [claro.data
                                   claro.data.ops
                                   claro.engine
                                   claro.projection]}}}
  :aliases {"codox" ["with-profile" "+codox" "codox"]}
  :pedantic? :abort)
