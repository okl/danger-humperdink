(ns humperdink.config.aws
  "Provide AWS config"
  {:author "Matt Halverson"
   :date "2014/07/29"}
  (:require [roxxi.utils.print :refer [print-expr]]
            [roxxi.utils.common :refer [def-]])
  (:require [clojure.tools.logging :as log])
  (:require [clojure.java.io :as io])
  (:require [clj-yaml.core :as yaml]))

(def- creds-path "conf/aws-creds.yml")
(def- creds-url (io/resource creds-path))
(defn s3-cred
  "From the aws.sdk.s3 library:

The credentials map should contain an :access-key key and a :secret-key key,
optionally an :endpoint key to denote an AWS endpoint and optionally a :proxy
key to define a HTTP proxy to go through."
  []
  (when (nil? creds-url)
    (let [msg (format "Missing aws-credentials resource: was looking for %s" creds-path)]
      (log/error msg)
      (throw (RuntimeException. msg))))
  (yaml/parse-string (slurp creds-url)))
