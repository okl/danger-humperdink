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
                                            make-log-to-disk]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def- act-reg (atom {}))

(defn defaction
  [name act]
  (when (contains? @act-reg name)
    (log/warnf "Attempting to overwrite already defined action %s" name))
  (swap! act-reg #(assoc % name act)))

(defn action-registry [] @act-reg)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;(defaction )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; (defn std-wrapper [fxn]
;;   (fn [request & args]
;;     (let [body (get :body request)]
;;       (apply fxn (list* body args)))))

(defn- parse-rf-pair [rf-pair]
  (let [path (first rf-pair)
        fxn (last rf-pair)]
    `(POST ~path req# (let [slurped# (assoc req# :body (slurp (get req# :body)))]
                        (apply ~fxn (list slurped#))))))
;; wrap the fxn binding the uri and any headers? maybe, but it feels odd
;; make humperdink functions take a request and destructure it? <-- THIS ONE!



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
;; (defpipeline playing-with-syntax-more
;;   [["/log-to-disk" => (make-log-to-disk "/tmp/foo")]
;;    ["/pipeline1" => p1]])
