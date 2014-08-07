(ns humperdink.pipeline
  "Declaratively define Humperdink pipelines"
  {:author "Matt Halverson"
   :date "2014/06/13"}
  (:require [compojure.core :refer [defroutes POST GET HEAD]])
  (:require [compojure.handler :as handler]
            [compojure.route :as route])
  (:require [ring.middleware.stacktrace :refer [wrap-stacktrace]])
  (:require [roxxi.utils.print :refer [print-expr]]
            [roxxi.utils.common :refer [def-]])
  (:require [clojure.tools.logging :as log])
  (:require [humperdink.actions.log :refer [log-to-disk-handler
                                            make-log-to-disk]]
            [humperdink.action :refer [invoke
                                       ->Env
                                       ->ActionInput]])
  (:use humperdink.actions.out-of-the-box))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- parse-rf-pair [rf-pair]
  (let [path (first rf-pair)
        action (last rf-pair)]
    `(POST ~path req# (let [body# (slurp (get req# :body))
                            slurped# (assoc req# :body body#)
                            route# (get req# :uri)
                            custom-fields# {}
                            env# (->Env route# custom-fields#)
                            input# (->ActionInput body# env#)
                            output# (invoke ~action input#)
                            outputs# (map #(:output %) output#)
                            ]
                        ;;(with-out-str (print-expr req#))
                        ;;(with-out-str (print-expr output#))
                        (with-out-str (clojure.pprint/pprint outputs#))
                        ))))

;; Still need to add `:ring {:handler humperdink.pipeline/~name}` to project.clj
;;               and `:plugins [[lein-ring "0.8.10"]]`
(defmacro defpipeline [name route-fn-pairs]
  (let [cleaned-rf-pairs (map parse-rf-pair route-fn-pairs)
        routes-name (symbol (str name "-routes"))]
    `(do
       (defroutes ~routes-name
         ~@cleaned-rf-pairs
         (HEAD "/" [] "") ;; Elastic Beanstalk sends a HTTP HEAD request to
                          ;; '/' to check if the application is running.
         (route/not-found "Route not found"))
       (def ~name
         (-> (handler/site ~routes-name)
             (wrap-stacktrace))))))

(def fn-1 (constantly "1\n"))
(def fn-2 (constantly "2\n"))
(def fn-3 (constantly "3\n"))
(def default-fn (constantly {}))

(defpipeline route-fn-registry
  [["/log-to-disk" => log-to-disk-handler]
   ["/foo/bar"     => fn-1]
   ["/foo"         => fn-2]
   ["/foo/bar/baz" => default-fn]
   ["/baz"         => default-fn]
   ["/bar/*"       => default-fn]
   ["/bar/*/baz/*" => fn-3]])

(defroutes r2
  (GET "/foo" [] (constantly "it works\n"))
  (GET "/bar" [] (constantly "it really works\n"))
  (HEAD "/" [] "")
  (route/not-found "Route not found"))
(def route-fn-registry
  (-> (handler/site r2)
      (wrap-stacktrace)))


(defpipeline route-fn-registry
  [["/foo" => basic-jaw]
   ["/yoda" => yodaetl]])

;; (defn wrap-correct-content-type
;;   "This is a something of a hack.
;;
;; The default content-type is NOT text/plain or application/json,
;; but rather application/www-form-urlencoded, which causes the body stream to
;; be automatically parsed into a map of parameters, thus consuming the stream...
;; so when the app tries to read it in order to log it, it's already been consumed
;; and there's nothing left to read and you get the empty string.
;;
;; In theory, people will hit this API with the correct content-type (probably
;; application/json or text/plain). If they're not, and you want a server-side
;; hack to accommodate them in their unhelpfulness, you can enable this by
;; uncommenting the call to (wrap-correct-content-type) in the app def below."
;;   [handler]
;;   (fn [request]
;;     (if (and (= "/log" (:uri request))
;;              (= :post (:request-method request)))
;;       (handler (assoc request :content-type "text/plain"))
;;       (handler request))))

;; There exists a route->action table
;; Interpreter has access to master route->action table
;;  -> first step is generic: look up action for route
;;  -> then interpret the value using that action
;;  -> the action may be composite, end with a "re-route" step
;;               ALTERNATIVELY
;; An action may be composite, end with a reference to another (possibly composite) action
;;               ALTERNATIVELY
;; Two-level interpreter
;;  -> inner level builds up a single giant function
;;  -> outer level picks the function to execute, 
