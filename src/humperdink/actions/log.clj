(ns humperdink.actions.log
  "Extend jawsome with some useful humperdink fns"
  {:author "Matt Halverson"
   :date "2014/06/13"}
  (:require [roxxi.utils.print :refer [print-expr]]
            [roxxi.utils.common :refer [def-]])
  (:require [clojure.tools.logging :as log])
  (:require [jawsome-dsl.xform :refer [defvar defxform]]
            [jawsome-dsl.denorm :refer [denorm-phase-sexprs->fn default-env]])
  (:require [humperdink.actions.data-log :refer [log]]))

;; (defn replace-newlines-with-spaces [m]
;;   (walk-update-scalars m
;;                        (fn [val]
;;                          (if (string? val)
;;                            (clojure.string/replace val #"\n" " ")
;;                            val))))

;; (defxform 'replace-newlines-with-spaces
;;   (constantly replace-newlines-with-spaces))

;; (defvar 'static-map static-map)

(defn- build-log-response [successfully-logged]
  (if successfully-logged
    {:status 200
     :headers {"Content-Type" "text/plain; charset=utf-8"}
     :body "Log succeeded\n"}
    {:status 500
     :headers {"Content-Type" "text/plain; charset=utf-8"}
     :body "Failed to log the entry\n"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; log to file: string to log, path to logfile (with some reasonable default?)

(defn- newlineify [s]
  (if (or (nil? s) (.endsWith ^String s "\n"))
    s
    (str s "\n")))

(defn log-to-disk [m path]
  (spit path (newlineify m) :append true))

(defn log-to-disk-handler [req]
  ;; XXX do some path checking here... don't let ppl write to arbitrary paths
  (let [{body :body, headers :headers} req
        local-path (get headers "local-path")]
    (log-to-disk body local-path)
    (build-log-response true)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; log to S3: string to log, path (bucket + key) to s3file
;;
;; ought to buffer, but that should be transparent.

(defn log-to-s3 [m bucket key]
  nil)
(defn- make-log-to-s3 [bucket key]
  #(log-to-s3 % bucket key))

;;(defxform 'log-to-s3 make-log-to-s3)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; push to AMQP: string to log, queue object (whatever that ends up meaning)

(defn push-to-amqp [m q]
  nil)

(defn make-push-to-amqp [q]
  #(push-to-amqp % q))

;;(defxform 'push-to-amqp make-push-to-amqp)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def d (denorm-phase-sexprs->fn
        '(denorm-phase (xform-phase (xforms :denorm)))
                                default-env))

d
