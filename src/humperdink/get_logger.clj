(ns humperdink.get-logger
  "Dynamically create new log4j Loggers"
  {:author "Matt Halverson"
   :date "2014/04/29"}
  (:require [roxxi.utils.print :refer [print-expr]]
            [roxxi.utils.common :refer [def-]])
  (:require [clojure.tools.logging :as log])
  (:import (org.apache.log4j PatternLayout
                             DailyRollingFileAppender
                             Logger
                             Level)))

(def pl
  (doto
      (new PatternLayout)
    (.setConversionPattern "%d{ISO8601} %-5p %c - %m%n")))

(def hourly-pattern "'.'yyyy-MM-dd-HH-mm")
(def default-date-pattern hourly-pattern)

(defn make-daily-rolling-file-appender [filepath]
  (log/info
   (format "Making new DailyRollingFileAppender. layout is %s, datePattern is %s, file is %s"
           pl
           default-date-pattern
           filepath))
  (doto
      (new DailyRollingFileAppender)
    (.setLayout pl)
    (.setDatePattern default-date-pattern)
    (.setFile filepath)
    (.activateOptions) ;; NB `activateOptions` will create an empty file to append to, if no logfile exists.
    ))

;; Q: What is the interplay between logger-name and appender-filepath?
;; A: If you make a logger with the same name twice, you will get back the same
;;     object
;;
;; humperdink.create-logger> (make-rolling-logger "a" "/tmp/a")
;; #<Logger org.apache.log4j.Logger@623b5968>
;; humperdink.create-logger> (make-rolling-logger "a" "/tmp/a")
;; #<Logger org.apache.log4j.Logger@623b5968>
;; humperdink.create-logger> (make-rolling-logger "a" "/tmp/b")
;; #<Logger org.apache.log4j.Logger@623b5968>

(defn make-rolling-logger
  "logger-name will appear in the log under the %c ConversionPattern option"
  [logger-name filepath]
  (let [appender (make-daily-rolling-file-appender filepath)]
    (doto
        (Logger/getLogger logger-name)
      (.setLevel Level/INFO)
      (.addAppender appender)
      (.setAdditivity false))))

;;TODO what about closing out the file handles, etc? Yes, put them in a cache,
;;     but how to destroy when evicting from the cache?
;; test with lsof?
;;
;; YES -- lsof | grep "humperdink" | less
;;
;; (def a (make-rolling-logger "a" "/Users/mhalverson/Code/okl/humperdink/logs/lsof_test.log"))
;; (comment "lsof reveals the open file handle")
;; (.removeAllAppenders a)
;; (comment "lsof reveals that the file handle is now closed")
;; (.info a "shouldn't write")
;; (def a (make-rolling-logger "a" "/Users/mhalverson/Code/okl/humperdink/logs/lsof_test.log"))
;; (comment "lsof reveals the open file handle again :)")
;; (.info a "should write")
;; (.info a "and it totally wrote")


;; (def l1 (make-rolling-logger "matt.test" "/Users/mhalverson/Code/okl/humperdink/logs/does_it_work2.log"))
;; (.info l1 "omfg this totally works")
;; (def l2 (make-rolling-logger "matt.other_test" "/Users/mhalverson/Code/okl/humperdink/logs/does_it_work_no_activate_options.log"))
;; (.info l2 "even without activating options")


;;TODO make logger cache with size limit, expiry policy, eviction mechanism
;;TODO make arbitrary paths
(def- log-pool
  (atom {}))

(defn get-logger [logger-name rel-filepath]
  (let [using-existing-logger (contains? @log-pool logger-name)
        filepath (str "logs/" rel-filepath)
        logger (if using-existing-logger
                 (get @log-pool logger-name)
                 (make-rolling-logger logger-name filepath))]
    (when-not using-existing-logger
      (log/info
       (format "Creating new logger with name %s and filepath %s.
Current number of loggers is %s.
Someday this may become a problem, if this becomes a real long-lived
TrackingAPI, due to OS limits on number of open filehandles per process."
               logger-name filepath (count @log-pool)))
      (swap! log-pool assoc logger-name logger))
    logger))
