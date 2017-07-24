(ns svc-herald.s3-backed-atom-test
  (:require [alandipert.enduro :as enduro]
            [amazonica.aws.s3 :as s3]
            [clojure.test :refer :all]
            [svc-herald.s3-backed-atom :refer :all]
            [java-time :as jt]))

(deftest ^:integration s3-atom-tests
  (let [bucket "svc-herald"
        filename (format "test/%s.json"
                         (jt/to-millis-from-epoch (jt/instant)))
        id1 (java.util.UUID/randomUUID)
        id2 (java.util.UUID/randomUUID)]
    (testing "Creation and adding"
      (let [atom (s3-atom {} bucket filename)]

        (testing "creation"
          (is (= @atom {}))
          (is (s3/does-object-exist bucket filename)))

        (testing "adding elements"
          (let [event1 {:id id1
                        :name "Friday evening sale"
                        :start (jt/zoned-date-time 2016 9 30 21 0 0 0 "UTC")
                        :duration (jt/duration 1 :hours)
                        :recurrence :weekly
                        :scale 1
                        :affected [:web :mobile]
                        :active true}
                event2 {:id id2
                        :name "Another event"
                        :start (jt/zoned-date-time 2016 9 30 21 0 0 0 "UTC")
                        :duration (jt/duration 1 :hours)
                        :recurrence :daily
                        :scale 0.75
                        :affected [:web]
                        :active true}]
            (enduro/swap! atom assoc (str id1) event1)
            (is (= (count @atom) 1))
            (enduro/swap! atom assoc (str id2) event2)
            (is (= (count @atom) 2))))))

    (testing "Restoring state"
      (let [atom (s3-atom {} bucket filename)]
        (is (= (count @atom) 2))
        (is (= (:name (@atom (str id1))) "Friday evening sale"))
        (is (= (:name (@atom (str id2))) "Another event"))))))
