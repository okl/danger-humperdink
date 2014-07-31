(ns humperdink.interp
  "Defines an action interpreter, for building up big complex Actions"
  {:author "Matt Halverson"
   :date "2014/07/10"}
  (:require [roxxi.utils.print :refer [print-expr]]
            [roxxi.utils.common :refer [def-]])
  (:require [humperdink.action :refer [invoke
                                       ActionP
                                       ->Action
                                       ->MultiAction
                                       ->ActionInput
                                       ->ActionOutput
                                       seqify]])
  (:require [clojure.tools.logging :as log])
  (:require [diesel.core :refer [definterpreter]])
  (:require [denormal.core :refer [denormalize-map]]))

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
(def begins-with-symbol? (comp symbol? first))

(definterpreter interp [reg]
  ['->   => :sequence]
  [':seq => :sequence]
  ['||        => :parallel]
  [':parallel => :parallel]
  ['ref  => :ref]
  [':ref => :ref]
  [begins-with-symbol? => :simple-action])

(defn- in->out [action-input]
  (->ActionOutput (:input action-input) (:env action-input)))
(defn- out->in [action-output]
  (->ActionInput (:output action-output) (:env action-output)))

(defmethod interp :sequence [[token & exprs] reg]
  ;; Daisy-chains a bunch of Actions together, feeding the ActionOutput(s)
  ;; of each Action as ActionInputs into the next."
  (println "called sequence")
  (let [sub-actions (map #(interp % reg) exprs)]
    (reify ActionP
      (invoke [_ action-input]
        (loop [prev-action-outputs (list (in->out action-input))
               actions sub-actions]
          (if (empty? actions)
            prev-action-outputs
            (let [curr-action (first actions)
                  curr-action-inputs (map out->in prev-action-outputs)]
              (recur (mapcat #(invoke curr-action %) curr-action-inputs)
                     (rest actions)))))))))

(defmethod interp :parallel [[token & exprs] reg]
  ;;require at least one branch.
  ;;the value is the value of the last branch.
  (println "called parallel")
  (when (empty? exprs)
    (log/errorf "Called %s with 0 args. Behavior will be undefined." token))
  (let [sub-actions (map #(interp % reg) exprs)]
    (reify ActionP
      (invoke [_ action-input]
        (last (pmap #(invoke % action-input) sub-actions))))))

(defmethod interp :ref [[_ name] reg]
  (println "called ref")
  (get reg name))

;; XXX here is where to honor "only" and "disable" options.
;; XXX here is where to honor "log every step" option?
(defmethod interp :simple-action [[name & init-args] reg]
  (println "called simple-action")
  (let [action-maker (get reg name)
        looked-up-refs (map #(interp % reg) init-args)
        action (apply action-maker looked-up-refs)]
    action))

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
      (->Action (fn [val env] [(str val " " init-arg) env]))))
  (defvar 'init-arg-99 "nevergonnagiveyouup")
  (def p1 (interp '(jawsome-xform-99 (ref init-arg-99))
                  (action-maker-registry)))
  p1
  (invoke p1 (->ActionInput 42 {})))

;; Parallel action

(do
  (defactionmaker 'print-1
    (fn []
      (->Action (fn [val env]
                  (println (str "gooo print-1, value is " val "!"))
                  ["print1" env]))))
  (defactionmaker 'print-2
    (fn []
      (->Action (fn [val env]
                  (println (str "huzzah print-2, value is " val "!"))
                  ["print2" env]))))
  (def p2 (interp '(|| (print-1)
                       (print-2))
                  (action-maker-registry)))
  (invoke p2 (->ActionInput 42 {})))

;; Composite action

(do
  (defactionmaker 'inc
    (fn []
      (->Action (fn [val env]
        [(inc val) env]))))
;;        (inc val))))
  (defactionmaker 'log
    (fn []
      (->Action (fn [val env]
                  (println (str "val is " val ", env is " env))
                  [val env]))))
;;        val)))
  (defactionmaker 'double
    (fn []
      (->Action (fn [val env]
                  [(* val 2) env]))))
;;        (* val 2))))
  (defactionmaker 'mult
    (fn [factor]
      (->Action (fn [val env]
                  [(* val factor) env]))))
;;        (* val factor))))
  (defactionmaker 'fork
    (fn []
      (->MultiAction (fn [val env]
                       [[val (assoc env :fork 1)]
                        [val (assoc env :fork 2)]]))))
  (def p3 (interp '(-> (log)
                       (double)
                       (log)
                       (mult 5)
                       (log)
                       (inc)
                       (log)
                       (fork)
                       (log))
                  (action-maker-registry)))
  (invoke p3 (->ActionInput 16 {})))

(do
  (defactionmaker 'p3
    (constantly p3))
  (def p4 (interp '(-> (p3)
                       (mult 10)
                       (log))
                  (action-maker-registry)))
  (invoke p4 (->ActionInput 16 {}))
  )

;; parallel of seqs

(do
  (defactionmaker 'read-json
    (fn []
      (->Action (fn [val env]
                  [(read-string val) env]))))
  (defactionmaker 'denorm
    (fn []
      (->MultiAction (fn [val env]
                       (let [vals (denormalize-map val)
                             val-env-pairs (map #(vector % env) vals)]
                         val-env-pairs)))))
  (def p-of-s (interp '(|| (|| (print-1)
                               (print-1))
                           (log)
                           (-> (read-json)
                               (denorm)
                               (log))
                           (-> (print-2)
                               (jawsome-xform-99 (ref init-arg-99))))
                  (action-maker-registry)))
  (invoke p-of-s (->ActionInput "{\"a\" [1 2 3], \"b\" 324}" {}))
  )


;; seq of parallels
(do
  (def s-of-p (interp '(-> (|| (log)
                               (-> (inc)
                                   (log)))
                           (|| (print-1)
                               (print-2)))
                      (action-maker-registry)))
  (invoke s-of-p (->ActionInput 42 {}))
  )
