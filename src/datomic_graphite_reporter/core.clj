;; Copyright 2014 Pellucid Analytics
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;     http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.


(ns datomic-graphite-reporter.core
  (:require [clojure.string :as str])
  (:import [java.net DatagramSocket InetSocketAddress]
           java.nio.ByteBuffer
           java.nio.channels.DatagramChannel
           java.nio.charset.Charset
           [org.slf4j Logger LoggerFactory]))


(def ^Logger logger
  (LoggerFactory/getLogger "datomic.graphite.reporter"))


(def ^String graphite-host
  (or
   (System/getProperty "graphite.host")
   (System/getenv      "GRAPHITE_HOST")))


(def ^String graphite-port
  (or
   (System/getProperty "graphite.port")
   (System/getenv      "GRAPHITE_PORT")))


(def ^String graphite-prefix
  (or
   (System/getProperty "graphite.prefix")
   (System/getenv      "GRAPHITE_PREFIX")))


(def ^Charset ISO-Latin-1-Charset
  (Charset/forName "ISO-8859-1"))


(def ^InetSocketAddress graphite-address
  (try
    (InetSocketAddress. graphite-host
                        (Integer/parseInt graphite-port))
    (catch Exception e
      (when (.isErrorEnabled logger)
        (.error logger
                "Failed to create socket address to Graphite!"
                e))
      (throw e))))


(defn- ^ByteBuffer build-graphite-metric-data
  [name value timestamp]
  (-> (str name
           \space
           value
           \space
           timestamp
           \newline)
      (.getBytes ISO-Latin-1-Charset)
      ByteBuffer/wrap))


(def ^DatagramChannel graphite-channel nil)


;; Inspired by com.codahale.metrics.graphite.GraphiteUDP
(defn- ^DatagramChannel get-graphite-channel
  []
  (if (and graphite-channel
           (not (-> graphite-channel
                    (.socket)
                    (.isClosed))))
    graphite-channel
    (do
      (when graphite-channel
        (.close graphite-channel))
      (let [chan
            (try
              (-> (DatagramChannel/open)
                  (.connect graphite-address))
              (catch Exception e
                (when (.isErrorEnabled logger)
                  (.error logger
                          "Failed to make a UDP connection to Graphite!"
                          e)
                  (throw e))))]
        (when (.isInfoEnabled logger)
          (.info logger (str "Opened connection to Graphite at "
                             graphite-address)))
        (alter-var-root #'graphite-channel
                        (constantly chan))
        chan))))


(defn- ^String unix-timestamp
  []
  (str (quot (System/currentTimeMillis) 1000)))


(defn- send-metric-data
  [^DatagramChannel chan metric-name metric-value timestamp]
  (try
    (.write chan
           (build-graphite-metric-data metric-name
                                       metric-value
                                       timestamp))
    (when (.isTraceEnabled logger)
      (.trace logger "Wrote metric data to Graphite."))
    (catch Exception e
      (when (.isErrorEnabled logger)
        (.error logger
                "Failed to write data to the Graphite channel!"
                e)
        (throw e)))))


(defn- ^String mk-metric-name
  [& rest]
  (str/join \.
            (if graphite-prefix
              (cons graphite-prefix rest)
              rest)))


(defn report-metrics
  [metrics]
  (let [timestamp (unix-timestamp)
        chan (get-graphite-channel)]
    (doseq [[metric-name metric-value] metrics]
      (if (map? metric-value)
        (doseq [[sub-metric-name sub-metric-value] metric-value]
          (send-metric-data chan
                            (mk-metric-name (name metric-name) (name sub-metric-name))
                            sub-metric-value
                            timestamp))
        (send-metric-data chan
                          (mk-metric-name (name metric-name))
                          metric-value
                          timestamp)))))

