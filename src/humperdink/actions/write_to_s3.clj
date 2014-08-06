(ns humperdink.actions.write-to-s3
  "Write loglines to s3 files"
  {:author "Matt Halverson"
   :date "2014/08/05"}
  (:require [roxxi.utils.print :refer [print-expr]]
            [roxxi.utils.common :refer [def-]])
  (:require [clojure.tools.logging :as log])
  (:require [humperdink.config.aws :refer [s3-cred]])
  (:require [clj-pid.core :as pid])
  (:require [aws.sdk.s3 :as s3]))


;; # Uploading known things

(def- cred (s3-cred))

(def bucket
  ;; "{{ BUCKET }}")
  ;; "okl-humperdink-logs")
  "okl-danger-dev-mhalverson")

(defn- generate-s3-key [route]
  (let [ts (System/currentTimeMillis)
        year-month-day (.format (java.text.SimpleDateFormat. "yyyy/MM/dd") ts)
        envt ;; "{{ ENVT }}"
                "dev"
        servername (.getHostName (java.net.InetAddress/getLocalHost))
        pid (pid/current)
        hour-min-sec (.format (java.text.SimpleDateFormat. "hh.mm.ss") ts)]
    (clojure.string/join "/" [year-month-day

                              envt
                              servername
                              pid

                              (subs route 1) ;; drop the starting /
                              hour-min-sec
                              ])))

(defn loaded-to-s3? [key]
  (s3/object-exists? cred bucket key))

(defn upload-to-s3! [vec-of-vals key]
  (let [string (clojure.string/join "\n" (map str (conj vec-of-vals "")))]
    (log/info (format "Uploading file to key %s" key))
    (if (loaded-to-s3? key)
      (log/info (format "Abandoning upload of key %s: already got written to s3" key)) ;; XXX is this ok policy? should retry with a different key?
      (s3/put-object cred bucket key string))))

;; # Deciding when to upload & managing content to upload

;; ## Individual buffers

(defprotocol S3BufferP
  (append [this val] "Add a value to the buffer")
  (flush-to-s3 [this] "Flush the buffer contents to a timestamped file in s3"))

(defrecord S3Buffer [vector-ref route]
  S3BufferP
  (append [this val]
    (dosync (commute vector-ref conj val)))
  (flush-to-s3 [this]
    ;; XXX write empty files to s3? so we know we're not missing uploads?
    ;;     or don't write if the vector is empty?
    (if (empty? @vector-ref)
      (log/debug "Nothing to flush")
      (let [s3-key (generate-s3-key route)
            vec-of-vals (dosync (let [v @vector-ref]
                                  (ref-set vector-ref [])
                                  v))]
        (log/debugf "Flushed a vector of %d items " (count vec-of-vals))
        (upload-to-s3! vec-of-vals s3-key)))))

(defn- make-s3-buffer [route]
  (->S3Buffer (ref []) route))

;; ## All s3-buffers

(def- route=>s3-buffer (ref {}))

(defn- set-up-flushing-s3-buffer [route & {:keys [flush-period-in-ms]
                                           :or {flush-period-in-ms 60000}}]
  (let [s3-buffer (make-s3-buffer route)
        ret-val (dosync
                 (when-not (contains? @route=>s3-buffer route)
                   (alter route=>s3-buffer assoc route s3-buffer)))
        had-to-set-up (not (nil? ret-val))]
    (when had-to-set-up
      (future (loop []
                ;;(Thread/sleep flush-period-in-ms) XXX
                (Thread/sleep 5000)
                (if (not (contains? @route=>s3-buffer route))
                  "terminated, finally!"
                  (do (flush-to-s3 (get @route=>s3-buffer route))
                      (recur))))))))

(defn add-to-s3-buffer
  "By default, this buffers all writes for a minute at a time;
it flushes the writes (if any) to timestamped s3 files every 60 seconds or so.

Note that the s3-keys will include the route, e.g. if this action is registered
under the '/testing/yoda' route, then the s3-key will include 'testing/yoda'."
  [val route]
  (when-not (contains? @route=>s3-buffer route)
    (set-up-flushing-s3-buffer route))
  (append (get @route=>s3-buffer route) val))
