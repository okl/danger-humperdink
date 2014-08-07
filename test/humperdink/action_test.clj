(ns humperdink.action-test
  "Test for action protocols"
  {:author "Matt Halverson"
   :date "2014/08/06"}
  (:require [roxxi.utils.print :refer [print-expr]]
            [roxxi.utils.common :refer [def-]])
  (:require [clojure.tools.logging :as log])
  (:use clojure.test)
  (:use humperdink.action))

;; # Sample action definitions

(deftest simple-action
  (testing "can we make an identity action"
    (let [a (->Action (fn [a b]
                        [a b]))
          i (->ActionInput 42 {})]
      (is (= (invoke a i)
             (list (->ActionOutput 42 {}))))))
  (testing "can we make an env-modifying action"
    (let [a (->Action (fn [val env]
                        [val (assoc env :it :works)]))
          i (->ActionInput 42 {})]
      (is (= (invoke a i)
             (list (->ActionOutput 42 {:it :works})))))))

(deftest multi-action
  (testing "basic multi-action"
    (let [a (->MultiAction (fn [val env]
                             [[val env]
                              [(inc val) (assoc env :i :gotcha)]
                              [(* 2 val) (assoc env :i :doubled)]]))
          i (->ActionInput 3 {})]
      (is (= (invoke a i)
             (list (->ActionOutput 3 {})
                   (->ActionOutput 4 {:i :gotcha})
                   (->ActionOutput 6 {:i :doubled})))))))
