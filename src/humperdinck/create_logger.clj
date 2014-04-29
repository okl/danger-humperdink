(ns humperdinck.create-logger
  "Dynamically create new log4j Loggers"
  {:author "Matt Halverson"
   :date "2014/04/29"}
  (:require [roxxi.utils.print :refer [print-expr]])
  (:require [clojure.tools.logging :as log])
  (:import (org.apache.log4j PatternLayout
                             DailyRollingFileAppender
                             Logger
                             Level)))

(def pl
  (doto
      (new PatternLayout)
    (.setConversionPattern "%d{ISO8601} %-5p %c - %m%n")))

(def hourly-pattern "'.'yyyy-MM-dd-HH")
(def default-date-pattern hourly-pattern)

(defn make-daily-rolling-file-appender [filepath]
  (doto
      (new DailyRollingFileAppender)
    (.setLayout pl)
    (.setDatePattern default-date-pattern)
    (.setFile filepath)
    (.activateOptions) ;; NB this will create the logfile
    ))

;; TODO what is the interplay between logger-name and appender-filepath?
;; if you make a logger with the same name twice, you will get back the same
;; object
;;
;; humperdinck.create-logger> (make-rolling-logger "a" "/tmp/a")
;; #<Logger org.apache.log4j.Logger@623b5968>
;; humperdinck.create-logger> (make-rolling-logger "a" "/tmp/a")
;; #<Logger org.apache.log4j.Logger@623b5968>
;; humperdinck.create-logger> (make-rolling-logger "a" "/tmp/b")
;; #<Logger org.apache.log4j.Logger@623b5968>

(defn make-rolling-logger
  "logger-name will appear in the log under the %c ConversionPattern option"
  [logger-name filepath]
  (let [appender (make-daily-rolling-file-appender filepath)]
    (doto
        (Logger/getLogger logger-name)
      (.setLevel Level/INFO)
      (.addAppender appender))))

;;TODO what about closing out the file handles, etc? Yes, put them in a cache,
;;     but how to destroy when evicting from the cache?
;; test with lsof?
;;
;; YES -- lsof | grep "humperdinck" | less
;;
;; (def a (make-rolling-logger "a" "/Users/mhalverson/Code/okl/humperdinck/logs/lsof_test.log"))
;; (comment "lsof reveals the open file handle")
;; (.removeAllAppenders a)
;; (comment "lsof reveals that the file handle is now closed")
;; (.info a "shouldn't write")
;; (def a (make-rolling-logger "a" "/Users/mhalverson/Code/okl/humperdinck/logs/lsof_test.log"))
;; (comment "lsof reveals the open file handle again :)")
;; (.info a "should write")
;; (.info a "and it totally wrote")


;; (def l1 (make-rolling-logger "matt.test" "/Users/mhalverson/Code/okl/humperdinck/logs/does_it_work2.log"))
;; (.info l1 "omfg this totally works")
;; (def l2 (make-rolling-logger "matt.other_test" "/Users/mhalverson/Code/okl/humperdinck/logs/does_it_work_no_activate_options.log"))
;; (.info l2 "even without activating options")


;;TODO make logger cache with size limit, expiry policy, eviction mechanism
;;TODO make arbitrary paths
