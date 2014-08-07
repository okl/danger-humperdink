(ns humperdink.action
  "Defines action protocols"
  {:author "Matt Halverson"
   :date "2014/07/10"}
  (:require [roxxi.utils.print :refer [print-expr]]
            [roxxi.utils.common :refer [def-]])
  (:require [clojure.tools.logging :as log]))

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
  (invoke [this action-input]
    "returns a seq of ActionOutputs"))

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
  ActionP
  (invoke [this action-input]
    (let [input (:input action-input)
          env (:env action-input)
          output-env-pairs (fxn input env)]
      (map #(->ActionOutput (first %) (second %)) output-env-pairs))))
