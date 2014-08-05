(ns humperdink.actions.out-of-the-box
  "Initialize the action-maker-registry with out-of-the-box stuff"
  {:author "Matt Halverson"
   :date "2014/08/01"}
  (:require [roxxi.utils.print :refer [print-expr]]
            [roxxi.utils.common :refer [def-]])
  (:require [humperdink.action :refer [invoke
                                       ActionP
                                       ->Action
                                       ->MultiAction
                                       ->ActionInput
                                       ->ActionOutput
                                       ->Env
                                       seqify]]
            [humperdink.interp :refer [defactionmaker
                                       defvar
                                       interp
                                       action-maker-registry]])
  (:require [denormal.core :refer [denormalize-map]])
  (:require [yodaetl.transform :as yoda]
            [yodaetl-hadoop.config :as yodacfg]))

;;

(defactionmaker 'log
  (fn []
    (->Action (fn [val env]
                (println (str "val is " val ", env is " env))
                [val env]))))

;; Just for now

(defactionmaker 'read-string
  (fn []
    (->Action (fn [val env]
                [(read-string val) env]))))
(defactionmaker 'denorm
  (fn []
    (->MultiAction (fn [val env]
                     (let [vals (denormalize-map val)
                           val-env-pairs (map #(vector % env) vals)]
                       val-env-pairs)))))


(def basic-jaw (interp '(-> (read-string)
                            (denorm))
                       (action-maker-registry)))

(defactionmaker 'basic-jaw
  (constantly basic-jaw))

(def- cfg-file (yodacfg/make-config-from-file "/Users/mhalverson/Code/okl/yodaetl/yodaetl/conf/yoda-denorm-config.yml"))

(defactionmaker 'yodaetl
  (fn []
    (->MultiAction (fn [val env]
                     (let [vals (yoda/transform val cfg-file)
                           val-env-pairs (map #(vector % env) vals)]
                       val-env-pairs)))))

(def yodaetl (interp '(yodaetl) (action-maker-registry)))
