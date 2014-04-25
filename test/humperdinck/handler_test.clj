(ns humperdinck.handler_test
  {:author "Matt Halverson"
   :date "2014/04/25"}
  (:use clojure.test
        ring.mock.request)
  (:require [humperdinck.handler :refer [app]]))

(deftest test-app
  (testing "main route"
    (let [response (app (request :get "/"))]
      (is (= (:status response) 200))
      (is (= (:body response) "Hello World"))))

  (testing "not-found route"
    (let [response (app (request :get "/invalid"))]
      (is (= (:status response) 404)))))
