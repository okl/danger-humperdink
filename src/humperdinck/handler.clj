(ns humperdinck.handler
  "Tracking API"
  {:author "Matt Halverson"
   :date "2014/04/25"}
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route])
  (:require [roxxi.utils.print :refer [print-expr
                                       print-prim]])
  (:require [clojure.java.io :refer [reader]]))

(defn log [thing]
  ;; log it
  ;; return appropriate status code -- 200 or 500
  (print-expr thing)
  nil)



(defroutes app-routes
  (GET "/" [] "Hello World")
  (POST "/log" {body :body} ;; TODO make this "/log/:service" [service] so you can provide a service-name
    (let [successfully-logged (log (slurp body))]
      (if successfully-logged
        "Log succeeded"
        {:status 500
         :headers {"Content-Type" "text/html; charset=utf-8"}
         :body "Failed to log the entry\n"})))
  (route/resources "/")
  (route/not-found "Not Found"))

;; (defn wrap-correct-content-type
;;   "This is a hack. The default content-type is NOT application/json,
;; but rather application/www-form-urlencoded, which causes the body stream to
;; be automatically parsed into a map of parameters, thus consuming the stream...
;; so when I try and read it"
;;   [handler]
;;   (fn [request]
;;     (handler (assoc request :content-type "application/json")))
;;   identity)

;; (def app
;;   (-> (handler/site app-routes)
;;       (wrap-correct-content-type)))
(def app
  (handler/site app-routes))

;; TODO don't barf stacktraces when exceptions get thrown D: it ain't secure
;; TODO document the fact that people MUST use content-type of application/json
;;      or text/plain when calling this api.
