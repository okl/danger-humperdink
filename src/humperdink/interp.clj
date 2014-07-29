(ns humperdink.interp
  "Defines an action interpreter"
  {:author "Matt Halverson"
   :date "2014/07/10"}
  (:require [roxxi.utils.print :refer [print-expr]]
            [roxxi.utils.common :refer [def-]])
  (:require [clojure.tools.logging :as log])
  (:require [diesel.core :refer [definterpreter]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def- act-maker-reg (atom {}))

(defn defactionmaker
  "act-maker takes some initialization args (possibly none) and returns a
function that takes exactly two args: the input value to operate on, and
an environment.

Haskell-style type signature:
act-maker :: [initialization args] -> (input -> env -> output)
"
  [name act-maker]
  (when (contains? @act-maker-reg name)
    (log/warnf "Attempting to overwrite already defined var %s" name))
  (swap! act-maker-reg #(assoc % name act-maker)))

(def defvar defactionmaker)

(defn action-maker-registry [] @act-maker-reg)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- seqify [thing]
  (if (or (seq? thing)
          (vector? thing))
    (seq thing)
    (list thing)))

(def begins-with-symbol? (comp symbol? first))

(definterpreter interp [reg]
  ['->   => :sequence]
  [':seq => :sequence]
  ['||        => :parallel]
  [':parallel => :parallel]
  ['ref  => :ref]
  [':ref => :ref]
  [begins-with-symbol? => :simple-action])

(defn- action-applier
  "Takes a collection of actions s.t. each action takes a value x env => value*
and returns a function that invokes each supplied function on value*

value* is short hand for a sequence of 0 or more values
"
  [actions]
  (fn [value* env]
    (loop [results (seqify value*)
           actions actions]
      (if (empty? actions)
        (seqify results)
        (recur (mapcat #((first actions) % env) results)
               (rest actions))))))
(defmethod interp :sequence [[_ & exprs] reg]
  (println "called sequence")
  (let [interped (map #(interp % reg) exprs)
        action-fn (action-applier interped)]
    (fn [val env]
      (action-fn val env))))

(defmethod interp :parallel [[token & exprs] reg]
  ;;require at least one branch.
  ;;the value is the value of the last branch.
  (println "called parallel")
  (when (empty? exprs)
    (log/errorf "Called %s with 0 args. Behavior will be undefined." token))
  (let [interped (map #(interp % reg) exprs)]
    (fn [val env]
      (seqify (last (pmap #(% val env) interped))))))

(defmethod interp :ref [[_ name] reg]
  (println "called ref")
  (get reg name))

(defmethod interp :simple-action [[name & init-args] reg]
  (println "called simple-action")
  (let [fxn-maker (get reg name)
        looked-up-refs (map #(interp % reg) init-args)
        action (apply fxn-maker looked-up-refs)]
    (comp seqify action)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Playing around with syntax

(def main-action
  '(-> (jawsome-xform-99 (ref init-arg-99))
       (|| (-> (jawsome-xform-1 init-arg-1 init-arg-2)
               (log-to-s3 bucket key))
           (-> (jawsome-xform-2 init-arg-3)
               (echo-to-console)
               (log-to-disk path)))))
(def main-action-2
  '(:seq (jawsome-xform-99 (:ref init-arg-99))
         (:parallel (:seq (jawsome-xform-1 init-arg-1 init-arg-2)
                          (log-to-s3 bucket key))
                    (:seq (jawsome-xform-2 init-arg-3)
                          (echo-to-console)
                          (log-to-disk path)))))

;; Test some basics!

;; Simple action

(do
  (defactionmaker 'jawsome-xform-99
    (fn [init-arg]
      (fn [val env] (str val " " init-arg " " env))))
  (defvar 'init-arg-99 "nevergonnagiveyouup")
  (def p1 (interp '(jawsome-xform-99 ["going home" (+ 2 2)])
                  (action-maker-registry)))
  (p1 42 {}))

;; Parallel action

(do
  (defactionmaker 'print-1
    (fn []
      (fn [val env]
        (println (str "gooo print-1, value is " val "!"))
        val)))
  (defactionmaker 'print-2
    (fn []
      (fn [val env]
        (println (str "huzzah print-2, value is " val "!"))
        (* val 2))))
  (def p2 (interp '(|| (print-1)
                       (print-2))
                  (action-maker-registry)))
  (p2 42 {}))

;; Composite action

(do
  (defactionmaker 'inc
    (fn []
      (fn [val env]
        (inc val))))
  (defactionmaker 'log
    (fn []
      (fn [val env]
        (println (str "val is " val))
        val)))
  (defactionmaker 'double
    (fn []
      (fn [val env]
        (* val 2))))
  (def p3 (interp '(-> (log)
                       (double)
                       (log)
                       (inc)
                       (log))
                  (action-maker-registry)))
  (p3 16 {}))
