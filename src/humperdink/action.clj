(ns humperdink.action
  "Defines action protocols"
  {:author "Matt Halverson"
   :date "2014/07/10"}
  (:require [roxxi.utils.print :refer [print-expr]]
            [roxxi.utils.common :refer [def-]])
  (:require [clojure.tools.logging :as log])
  ;;(:require [humperdink.actions.log :as log-action])
  )

(defn seqify [thing]
  (if (or (seq? thing)
          (vector? thing))
    (seq thing)
    (list thing)))

;; # Type definitions

(defrecord Env [route custom-fields])
(defrecord ActionInput [input env])
(defrecord ActionOutput [output env])

(defprotocol ActionP
  "docstring"
  (invoke [this ^ActionInput action-input] "returns a seq of ActionOutputs"))

(defrecord Action [fxn]
  ;; The fxn should take an input and env,
  ;; and return an output and possibly different env.
  ActionP
  (invoke [this action-input]
    (let [input (:input action-input)
          env (:env action-input)
          [output env'] (fxn input env)]
      (list (->ActionOutput output env')))))

(defrecord MultiAction [fxn]
  ;; The fxn should take an input and env,
  ;; and return a seq of output/possibly-different-env pairs.
  ;; A la denormalizing an array.
  ActionP
  (invoke [this action-input]
    (let [input (:input action-input)
          env (:env action-input)
          output-env-pairs (fxn input env)]
      (map #(->ActionOutput (first %) (second %)) output-env-pairs))))

;; # Sample action definitions

;;(defaction log-to-disk log-action/make-log-to-disk)

(def a1 (->Action
         (fn [a b]
           [(str a) {}])))
(invoke a1 (->ActionInput 42 {}))

(def a-identity
  (->Action (fn [val env]
              [val (assoc env :t (System/currentTimeMillis))])))
(invoke a-identity (->ActionInput 42 {}))

(def multi-a
  (->MultiAction (fn [val env]
                   [[val env]
                    [(inc val) (assoc env :i :gotcha)]
                    [(* 2 val) (assoc env :i :doubled)]])))
(invoke multi-a (->ActionInput 3 {}))
