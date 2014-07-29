(ns humperdink.action
  "Defines action protocols"
  {:author "Matt Halverson"
   :date "2014/07/10"}
  (:require [roxxi.utils.print :refer [print-expr]]
            [roxxi.utils.common :refer [def-]])
  (:require [clojure.tools.logging :as log])
  ;;(:require [humperdink.actions.log :as log-action])
  )


;; (defprotocol EnvP
;;   ""
;;   (route [this] ""))

;; (defprotocol ActionResultP
;;   ""
;;   (input [this] "")
;;   (value [this] "")
;;   (env [this] "")
;;   (message [this] ""))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ??
(defrecord Env [route custom-fields])

(defprotocol ActionP
  "docstring"
  (invoke [this input ^Env env] "docstring"))

(deftype Action [fxn]
  ActionP
  (invoke [this input env]
    (fxn input env)))

(defrecord ActionResult [input output ^Env env message])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;(defaction log-to-disk log-action/make-log-to-disk)

(def a1 (->Action str {}))
;;(defaction a1 [val env] val)
(def a-identity (->Action (fn [val env] val)
                  {}))
(value a-identity 42)
