(ns humperdink.interp-test
  {:author "Matt Halverson"
   :date "2014/08/01"}
  (:use clojure.test)
  (:require [roxxi.utils.common :refer [def-]]
            [roxxi.utils.print :refer [print-expr]])
  (:require [humperdink.interp :refer [defactionmaker
                                       defvar
                                       interp
                                       action-maker-registry]]
            [humperdink.action :refer [invoke
                                       ActionP
                                       ->Action
                                       ->MultiAction
                                       ->ActionInput
                                       ->ActionOutput
                                       ->Env
                                       seqify]])
  (:use humperdink.actions.out-of-the-box)
  (:require [denormal.core :refer [denormalize-map]]))


(def- empty-env (->Env "" {}))

(defn- make-o [output env]
  (->ActionOutput output env))

;; Test some basics!

(defactionmaker 'simple-action
  (fn [init-arg]
    (->Action (fn [val env] [(str val " " init-arg) env]))))
(defvar 'init-arg-1 "nevergonnagiveyouup")

(deftest simple-action
  (testing "simple-action"
    (let [a (interp '(simple-action (ref init-arg-1))
                    (action-maker-registry))
          i (->ActionInput 42 {})
          o (invoke a i)]
      (is (= o
             (list (->ActionOutput "42 nevergonnagiveyouup" {})))))))

;; Parallel action

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

(deftest parallel-action
  (testing "parallel action always returns the last value"
    (let [a (interp '(|| (print-1)
                         (print-2))
                    (action-maker-registry))
          i (->ActionInput 42 {})]
      (with-out-str ;; discard the string output
        (is (every? #(= (:output %) "print2")
                    (flatten (repeat 1000 (invoke a i)))))))))

;; Sequential action

(defactionmaker 'inc
  (fn []
    (->Action (fn [val env]
                [(inc val) env]))))
(defactionmaker 'double
  (fn []
    (->Action (fn [val env]
                [(* val 2) env]))))
(defactionmaker 'mult
  (fn [factor]
    (->Action (fn [val env]
                [(* val factor) env]))))
(defactionmaker 'fork
  (fn []
    (->MultiAction (fn [val env]
                     [[val (assoc env :fork 1)]
                      [val (assoc env :fork 2)]]))))
(def- seq-action (interp '(-> (double)
                             (mult 5)
                             (fork)
                             (fork)
                             (inc))
                        (action-maker-registry)))

(deftest sequential-action
  (testing "sequential action works"
    (let [a seq-action
          i (->ActionInput 16 {})]
      (is (every? #(= (:output %) 161)
                  (invoke a i))))))

;;

(deftest actions-can-be-reused
  (defactionmaker 'reused-action
    (constantly seq-action))
  (testing "actions created with interp can be used with defactionmaker"
    (let [a (interp '(-> (reused-action)
                         (mult 10))
                    (action-maker-registry))
          i (->ActionInput 16 {})]
      (is (every? #(= (:output %) 1610)
                  (invoke a i))))))

;; parallel of seqs

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

(deftest parallel-of-seqs
  (testing "parallel-of-sequential"
    (with-out-str ;; don't print to stdout
      (let [a (interp '(|| (-> (print-1)
                               (print-1))
                           (-> (read-string)
                               (denorm)))
                      (action-maker-registry))
            i (->ActionInput "{\"a\" [0 1 2], \"b\" 324}" {})]
        (is (= (set (map #(:output %) (invoke a i)))
               (hash-set {"b" 324, "a_arr" 0, "a_idx" 0}
                         {"b" 324, "a_arr" 1, "a_idx" 1}
                         {"b" 324, "a_arr" 2, "a_idx" 2})))))))

;;  seq of parallels

(deftest seq-of-parallels
  (testing "sequential-of-parallels"
    (let [a (interp '(-> (|| (-> (double)
                                 (inc))
                             (-> (inc)
                                 (inc)
                                 (inc)))
                         (|| (mult 1000)
                             (mult 11)))
                    (action-maker-registry))
          i (->ActionInput 42 {})]
      (is (= 495
             (:output (first (invoke a i))))))))
