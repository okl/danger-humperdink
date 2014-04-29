(ns humperdinck.handler
  "Tracking API"
  {:author "Matt Halverson"
   :date "2014/04/25"}
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route])
  (:require [roxxi.utils.print :refer [print-expr]])
  (:require [clojure.java.io :refer [reader]])
  (:require [clojure.tools.logging :as log])
  (:require [humperdinck.data-log :as data-log]))

(defn- build-log-response [successfully-logged]
  (if successfully-logged
    {:status 200
     :headers {"Content-Type" "text/plain; charset=utf-8"}
     :body "Log succeeded\n"}
    {:status 500
     :headers {"Content-Type" "text/plain; charset=utf-8"}
     :body "Failed to log the entry\n"}))

(defroutes app-routes
  (GET "/" [] "Hello World")
  ;; TODO do we want a "/log" route, with no :service param?
  ;; (POST "/log/:service" [service :as {body :body}]
  ;;   (let [successfully-logged (data-log/log service (slurp body))]
  ;;     (build-log-response successfully-logged)))
  (POST "/log/*" [:as {body :body, uri :uri}]
    (let [rel-path (subs uri 5)
          msg (slurp body)
          successfully-logged (data-log/log rel-path msg)]
      (build-log-response successfully-logged)))
  (route/resources "/")
  (route/not-found "Not Found"))



(defn wrap-correct-content-type
  "This is a something of a hack.

The default content-type is NOT text/plain or application/json,
but rather application/www-form-urlencoded, which causes the body stream to
be automatically parsed into a map of parameters, thus consuming the stream...
so when the app tries to read it in order to log it, it's already been consumed
and there's nothing left to read and you get the empty string.

In theory, people will hit this API with the correct content-type (probably
application/json or text/plain). If they're not, and you want a server-side
hack to accommodate them in their unhelpfulness, you can enable this by
uncommenting the call to (wrap-correct-content-type) in the app def below."
  [handler]
  (fn [request]
    (if (and (= "/log" (:uri request))
             (= :post (:request-method request)))
      (handler (assoc request :content-type "text/plain"))
      (handler request))))

(def app
  (-> (handler/site app-routes)
      ;; (wrap-correct-content-type)
      ))
