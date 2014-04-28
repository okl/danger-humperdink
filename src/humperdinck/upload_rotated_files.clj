(ns humperdinck.upload-rotated-files
  "Looks for rotated log-files and uploads them to Amazon S3"
  {:author "Matt Halverson"
   :date "2014/04/25"}
  (:require [roxxi.utils.print :refer [print-expr]])
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :refer [file
                                     delete-file]])
  (:require [clojure.java.shell :as shell])
  (:require [clj-pid.core :as pid])
  (:require [aws.sdk.s3 :as s3]))

;; # Globals, because sometimes it's just easier to do it that way

(def ^:dynamic target-dir)
(def ^:dynamic target-file)

;; # Helpers

;; ## List files

(defn- fileify [f-or-str]
  (if (string? f-or-str)
    (file f-or-str)
    f-or-str))

(defn- ls-recursive [dir]
  (file-seq (fileify dir)))

(defn- target-path []
  (format "%s/%s" target-dir target-file))

(defn- make-rotated-uncompressed-regex []
  (re-pattern (str (target-path) "\\..+")))
(defn- rotated-uncompressed-files []
  (let [regex (make-rotated-uncompressed-regex)]
    (->> (ls-recursive target-dir)
         (remove #(.isDirectory %))
         (remove #(.endsWith (.getPath %) ".gz"))
         (filter #(re-find regex (.getPath %))))))

(defn- make-rotated-compressed-regex []
  (re-pattern (str (target-path) "\\..+\\.gz")))
(defn- rotated-compressed-files []
  (let [regex (make-rotated-compressed-regex)]
    (->> (ls-recursive target-dir)
         (remove #(.isDirectory %))
         (filter #(.endsWith (.getPath %) ".gz"))
         (filter #(re-find regex (.getPath %))))))

;; ## Compress stuff

(defn compress! [file]
  (shell/sh "gzip" (.getPath file)))

;; ## S3 stuff

(def cred
  ;; {:access-key "{{ ACCESS_KEY }}",
  ;;  :secret-key "{{ SECRET_KEY }}"}
  nil)
(def service-name
  ;;"{{ SERVICE_NAME }}"
  )
(def bucket
  ;;(format "okl-%s-logs" service-name))
  "okl-danger-dev-mhalverson")

(def threshold (* 5 1024 1024 1024))
(def part-size (* 1024 1024 1024))

(defn- generate-s3-key [file]
  (let [filepath (.getPath file)
        regex (re-pattern (str target-dir "/" target-file "."
                               "(\\d{4,4})-(\\d{2,2})-(\\d{2,2})-(\\d{2,2})"
                               ".gz"))
        regex-matches (re-find regex filepath)
        [_ year month day hour] regex-matches
        envt ;;"{{ ENVT }}"
               "TEST"
        servername (.getHostName (java.net.InetAddress/getLocalHost))
        pid (pid/current)]
    (when (not= 5 (count regex-matches))
      (throw
       (RuntimeException.
        (format "Regex didn't match: filepath was %s, regex-matches was %s"
                filepath regex-matches))))
    (clojure.string/join "/"
                         [year
                          month
                          day
                          hour
                          envt
                          servername
                          pid
                          (.getName file)])))

(defn loaded-to-s3? [file]
  (let [key (generate-s3-key file)
        path (.getAbsolutePath file)]
    (log/info (format "Checking if file %s has been uploaded to s3 under key %s" path key))
    (if (s3/object-exists? cred bucket key)
      (do
        (log/info (format "Yes, file %s has been uploaded to s3 under key %s" path key))
        true)
      (do
        (log/info (format "No, file %s has not been uploaded to s3 under key %s" path key))
        false))))

(defn upload-to-s3! [file]
  (let [key (generate-s3-key file)]
    (log/info (format "Uploading file %s to key %s" (.getPath file) key))
    (if (> (.length file) threshold)
      (s3/put-multipart-object cred bucket key file :part-size part-size)
      (s3/put-object           cred bucket key file))))


;; TODO enable me in prod
(def delete-mode false)

(defn delete! "on localhost's filesystem" [file]
  (if delete-mode
    (do
      (log/info (format "Deleting file %s on localhost" (.getPath file)))
      (delete-file file))
    (do
      (log/info (format "Delete mode is disabled; not deleting file %s" (.getPath file))))))


(defn create-bucket-if-not-exists! [bucket]
  (if (not (s3/bucket-exists? cred bucket))
    (do
      (log/info (format "Creating bucket %s" bucket))
      (s3/create-bucket cred bucket))
    (log/info (format "No need to create bucket %s; it already exists" bucket))))


;; # Main

;; TODO need to check if the upload was successful - retry policy etc
(defn -main []
  (binding [target-dir "./logs"
            target-file "tracking_api_data.log"]
    (create-bucket-if-not-exists! bucket)
    (doseq [uncompressed (rotated-uncompressed-files)]
      (compress! uncompressed))
    (doseq [compressed (rotated-compressed-files)]
      (if (loaded-to-s3? compressed)
        (delete! compressed)
        (upload-to-s3! compressed)))
    ))

;; # Testing code

(println "================================================================================")
(println "JUST EVALED AGAIN")
(binding [target-dir "./logs"
          target-file "tracking_api_data.log"]
  (clojure.pprint/pprint (ls-recursive target-dir))
  (clojure.pprint/pprint (rotated-uncompressed-files))
  (clojure.pprint/pprint (rotated-compressed-files)))
(-main)


;; TODO cron this with lein run -m ..., add this as main to project.clj
