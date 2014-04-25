(ns humperdinck.handler
  "Tracking API"
  {:author "Matt Halverson"
   :date "2014/04/25"}
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route])
  (:require [roxxi.utils.print :refer [print-expr]]))

;; (defroutes app-routes
;;   (GET "/" [] "<h1>Hello World</h1>")
;;   (route/not-found "<h1>Page not found</h1>"))

(defroutes app-routes
  (GET "/" [] "Hello World")
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes)
  ;; (-> (handler/site app-routes)
  ;;     (wrap-base-url))
  )
