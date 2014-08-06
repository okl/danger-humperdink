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
  (:require [diesel.core :refer [definterpreter]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def- act-maker-reg (atom {}))

(defn defactionmaker
  "act-maker takes some initialization args (possibly none) and returns a
function that takes exactly two args: the input value to operate on, and
an environment.

Haskell-style type signature:
act-maker :: [initialization args] -> ActionP
"
  [name act-maker]
  (when (contains? @act-maker-reg name)
    (log/warnf "Attempting to overwrite already defined var %s" name))
  (swap! act-maker-reg assoc name act-maker))

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
  (when (empty? exprs)
    (log/errorf "Called %s with 0 args. Behavior will be undefined." token))
  (let [sub-actions (map #(interp % reg) exprs)]
    (reify ActionP
      (invoke [_ action-input]
        (last (pmap #(invoke % action-input) sub-actions))))))

(defmethod interp :ref [[_ name] reg]
  (let [looked-up (get reg name)]
    (if (nil? looked-up)
      (throw (RuntimeException. (format "Trying to reference undefined symbol %s" name)))
      looked-up)))

;; XXX here is where to honor "only" and "disable" options.
;; XXX here is where to honor "log every step" option?
(defmethod interp :simple-action [[name & init-args] reg]
  (let [action-maker (interp (list 'ref name) reg)
        looked-up-refs (map #(interp % reg) init-args)
        action (apply action-maker looked-up-refs)]
    action))
