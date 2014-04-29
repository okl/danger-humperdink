(ns humperdinck.data-log
  "Special ns in which all the log statements will write to a special separate log file"
  {:author "Matt Halverson"
   :date "2014/04/25"}
  (:require [roxxi.utils.print :refer [print-expr
                                       print-prim]])
  (:require [clojure.tools.logging :as log])
  (:require [humperdinck.get-logger :refer [get-logger]])
  (:require [clj-pid.core :as pid]))

;; TODO move host/pid calculation to the lein-server-startup thunk.

(defn log
  "Returns something falsy if the log failed, something truthy if it succeeded"
  ;;[service thing-to-log]
  ;; (let [host (.getHostName (java.net.InetAddress/getLocalHost))
  ;;       pid (pid/current)]
  ;;   (log/info (format "%s %s - [%s] - %s" host pid service thing-to-log))
  ;;   true)
  [^String service-path thing-to-log]
  (let [host (.getHostName (java.net.InetAddress/getLocalHost))
        pid (pid/current)
        logger-name (clojure.string/replace service-path #"/" ".")
        filepath (str service-path ".log")
        logger (get-logger logger-name filepath)]
    (.info logger (format "%s %s - %s" host pid thing-to-log))
    true))
