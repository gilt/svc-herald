(ns svc-herald.utils-test
  (:require [svc-herald.configuration :as cnf]
            [svc-herald.utils :as utils]
            [clojure.test :refer :all]))

(defn- count-calls [count-atom action]
  (swap! count-atom inc)
  (action))

(deftest retry-test
  (let [retry-config {:attempts 3
                      :delay 1
                      :scatter 1}]
    (testing "Retries when inner thunk throws an exception"
      (let [retries (atom 0)
            action #(throw (Exception. "This is a test"))
            call-count-action #(count-calls retries action)]

        (is (thrown-with-msg? Exception #"This is a test" (utils/retry retry-config call-count-action)))
        (is (= @retries (inc (:attempts retry-config))))))

    (testing "No retries when there was no exceptions"
      (let [retries (atom 0)
            action (fn [] "Success return from function")
            call-count-action #(count-calls retries
                                            action)
            result (utils/retry retry-config call-count-action)]

        (is (= result (action)))
        (is (= @retries 1))))

    (testing "Retries and returns the result on successfull attempt"
      (let [retries (atom 0)
            return-value "Success return from function"
            action #(if (< @retries 3)
                      (throw (Exception. "This is a test"))
                      return-value)
            call-count-action #(count-calls retries
                                            action)
            result (utils/retry retry-config call-count-action)]
        (is (= result return-value)
        (is (= @retries 3)))))))
