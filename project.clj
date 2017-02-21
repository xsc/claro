(defproject claro "0.2.9"
  :description "claro que sí"
  :url "https://github.com/xsc/claro"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"
            :year 2015
            :key "mit"}
  :dependencies [[org.clojure/clojure "1.8.0" :scope "provided"]
                 [org.clojure/core.async "0.2.395" :scope "provided"]
                 [potemkin "0.4.3"]
                 [manifold "0.1.5"]]
  :profiles {:dev
             {:dependencies [[org.clojure/test.check "0.9.0"]
                             [com.gfredericks/test.chuck "0.2.7"]
                             [instaparse "1.4.5"]]}
             :benchmarks
             {:plugins [[perforate "0.3.4"]]
              :dependencies [[perforate "0.3.4"]
                             [criterium "0.4.4"]
                             [muse "0.4.3-alpha3" :exclusions [manifold]]
                             [cats "0.4.0"]
                             [org.clojure/core.async "0.2.395"]
                             [funcool/urania "0.1.1"]
                             [funcool/promesa "1.7.0"]]
              :source-paths ["benchmarks"]
              :perforate
              {:environments
               [{:name :comparison
                 :namespaces [claro.resolution-without-batching.claro
                              claro.resolution-without-batching.urania
                              claro.resolution-without-batching.muse
                              claro.resolution-without-batching.assertion
                              claro.resolution-without-batching
                              claro.resolution-with-batching.claro
                              claro.resolution-with-batching.urania
                              claro.resolution-with-batching.muse
                              claro.resolution-with-batching.assertion
                              claro.resolution-with-batching]}
                {:name :performance
                 :namespaces [claro.resolution-without-batching.claro
                              claro.resolution-with-batching.claro
                              claro.projection-benchmarks.deep
                              claro.projection-benchmarks.union]}
                {:name :projections
                 :namespaces [claro.projection-benchmarks.deep
                              claro.projection-benchmarks.union]}]}
              :jvm-opts ^:replace ["-server" "-XX:+TieredCompilation"]}
             :codox
             {:dependencies [[org.clojure/tools.reader "1.0.0-beta2"]
                             [codox-theme-rdash "0.1.1"]]
              :plugins [[lein-codox "0.10.3"]]
              :codox {:project {:name "claro"}
                      :metadata {:doc/format :markdown}
                      :themes [:rdash]
                      :source-paths ["src"]
                      :source-uri "https://github.com/xsc/claro/blob/master/{filepath}#L{line}"
                      :namespaces [claro.data
                                   claro.data.ops
                                   claro.engine
                                   claro.engine.adapter
                                   claro.engine.selector
                                   #"^claro\.middleware\..*"
                                   claro.projection]}}}
  :aliases {"codox" ["with-profile" "codox,dev" "codox"]}
  :pedantic? :abort)
