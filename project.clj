(defproject humperdinck "0.1.0-SNAPSHOT"
  :description "Tracking API -- it can track a falcon on a cloudy day!"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.onekingslane.danger/clojure-common-utils "0.0.24"]
                 [compojure "1.1.6"]
                 [clj-pid "0.1.1"]
                 [org.clojure/tools.logging "0.2.6"]
                 [org.slf4j/slf4j-log4j12 "1.7.1"]]
  :plugins [[lein-ring "0.8.10"]]
  :ring {:handler humperdinck.handler/app
         ;; :init thunk
         ;; :destroy thunk
         ;; :adapter options-map
         }
  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                                  [ring-mock "0.1.5"]]}})
