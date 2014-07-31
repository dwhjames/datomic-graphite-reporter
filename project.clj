(defproject com.pellucid/datomic-graphite-reporter "1.0.0"
  :description "A Clojure library that reports Datomic transactor metrics to Graphite"
  :url "https://github.com/pellucidanalytics/datomic-graphite-reporter"
  :license {:name "Apache-2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.slf4j/slf4j-api "1.7.7"]]
  :jvm-opts ["-Dgraphite.host=localhost"
             "-Dgraphite.port=2003"
             "-Dgraphite.prefix=datomic"])

