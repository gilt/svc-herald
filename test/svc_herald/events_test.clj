(ns svc-herald.events-test
  (:require [java-time :as jt]
            [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [svc-herald.events :refer :all]
            [svc-herald.utils :refer [ny-time-zone utc-time-zone]]))

(deftest events-conversion-test
  (testing "closest-occurence-before"
    (let [func (var svc-herald.events/closest-occurence-before)]
      (let [result (func (jt/zoned-date-time 2016 10 7 18 0 0 0 ny-time-zone) ;Fri 10'Oct, 18:00 NY
                         (jt/weeks 1)
                         (jt/zoned-date-time 2016 11 15 12 30 0 0 utc-time-zone))]
        (is (= result (jt/zoned-date-time 2016 11 11 18 0 0 0 ny-time-zone))))

      (let [result (func (jt/zoned-date-time 2016 10 7 18 0 0 0 ny-time-zone) ;Fri 10'Oct, 18:00 NY
                         (jt/weeks 1)
                         (jt/zoned-date-time 2016 10 8 12 30 0 0 ny-time-zone))]
        (is (= result (jt/zoned-date-time 2016 10 7 18 0 0 0 ny-time-zone))))

      (let [result (func (jt/zoned-date-time 2016 10 7 18 0 0 0 ny-time-zone) ;Fri 10'Oct, 18:00 NY
                         (jt/weeks 1)
                         (jt/zoned-date-time 2016 10 5 12 30 0 0 ny-time-zone))]
        (is (= result nil)))

      (let [result (func (jt/zoned-date-time 2016 10 7 18 0 0 0 ny-time-zone) ;Fri 10'Oct, 18:00 NY
                         (jt/days 1)
                         (jt/zoned-date-time 2016 10 7 19 0 0 0 ny-time-zone))]
        (is (= result (jt/zoned-date-time 2016 10 7 18 0 0 0 ny-time-zone))))

      (let [result (func (jt/zoned-date-time 2016 10 7 18 0 0 0 ny-time-zone) ;Fri 10'Oct, 18:00 NY
                         (jt/days 1)
                         (jt/zoned-date-time 2016 11 15 13 0 0 0 utc-time-zone))]
        (is (= result (jt/zoned-date-time 2016 11 14 18 0 0 0 ny-time-zone))))

      (let [result (func (jt/zoned-date-time 2016 10 7 18 0 0 0 ny-time-zone) ;Fri 10'Oct, 18:00 NY
                         (jt/days 1)
                         (jt/zoned-date-time 2016 10 5 13 0 0 0 ny-time-zone))]
        (is (= result nil)))))

  (testing "ranges-intersects"
    (let [ranges-intersects? (var svc-herald.events/ranges-intersects?)
          test-range [(jt/zoned-date-time 2016 10 1 12 0 0 0 ny-time-zone)
                      (jt/zoned-date-time 2016 10 1 13 0 0 0 ny-time-zone)]
          range1 [(jt/zoned-date-time 2016 10 1 1 0 0 0 ny-time-zone)
                  (jt/zoned-date-time 2016 10 1 23 0 0 0 ny-time-zone)]
          range2 [(jt/zoned-date-time 2016 10 1 11 30 0 0 ny-time-zone)
                  (jt/zoned-date-time 2016 10 1 12 20 0 0 ny-time-zone)]
          range3 [(jt/zoned-date-time 2016 10 1 12 40 0 0 ny-time-zone)
                  (jt/zoned-date-time 2016 10 1 13 15 0 0 ny-time-zone)]
          range4 [(jt/zoned-date-time 2016 10 1 15 00 0 0 ny-time-zone)
                  (jt/zoned-date-time 2016 10 1 16 15 0 0 ny-time-zone)]]
      (is (ranges-intersects? test-range range1))
      (is (ranges-intersects? test-range range2))
      (is (ranges-intersects? test-range range3))
      (is (false? (ranges-intersects? test-range range4)))))

  (testing "expanding description into events"
    (testing "daily repeating sales"
      (let [description {:name "Noon sale"
                         :start (jt/zoned-date-time 2016 10 1 12 0 0 0 ny-time-zone)
                         :duration (jt/duration 1 :hours)
                         :recurrence :daily
                         :scale 1
                         :affected-area [:web :mobile]}
            result (expand-description-into-events
                    description
                    (jt/zoned-date-time 2016 10 6 1 0 0 0 utc-time-zone)
                    (jt/zoned-date-time 2016 10 7 1 0 0 0 utc-time-zone))]
        (is (= 1 (count result)))
        (let [head-desc (first result)]
          (is (= (:begin head-desc) (jt/instant "2016-10-06T16:00:00Z")))
          (is (= (:end head-desc) (jt/instant "2016-10-06T17:00:00.000Z")))
          (is (= (:scale head-desc) 1)))))

    (testing "returns nothing if the start date way ahead"
      (let [description {:name "Noon sale"
                         :start (jt/zoned-date-time 2016 12 1 12 0 0 0 ny-time-zone)
                         :duration (jt/duration 1 :hours)
                         :recurrence :daily
                         :scale 1
                         :affected-area [:web :mobile]}
            result (expand-description-into-events
                    description
                    (jt/zoned-date-time 2016 11 10 12 0 0 0 ny-time-zone)
                    (jt/zoned-date-time 2016 11 11 12 0 0 0 ny-time-zone))]
        (is (= 0 (count result)))))

    (testing "weekly repeating sales returns the sale in the result if it close enough"
      (let [description {:name "Friday evening sale"
                         :start (jt/zoned-date-time 2016 9 30 21 0 0 0 ny-time-zone)
                         :duration (jt/duration 1 :hours)
                         :recurrence :weekly
                         :scale 1
                         :affected-area [:web :mobile]}
            result (expand-description-into-events
                    description
                    (jt/zoned-date-time 2016 10 14 12 0 0 0 utc-time-zone)
                    (jt/zoned-date-time 2016 10 15 12 0 0 0 utc-time-zone))]
        (is (= (count result) 1))
        (let [{begin :begin
               end :end
               scale :scale} (first result)]
          (is (= begin (jt/instant "2016-10-15T01:00:00Z")))
          (is (= end (jt/instant "2016-10-15T02:00:00Z")))
          (is (= scale 1)))))

    (testing "weekly repeating sale returns no result in case it is far away"
      (let [description {:name "Friday evening sale"
                         :start (jt/zoned-date-time 2016 9 30 21 0 0 0 ny-time-zone)
                         :duration (jt/duration 1 :hours)
                         :recurrence :weekly
                         :scale 1
                         :affected-area [:web :mobile]}
            result (expand-description-into-events
                    description
                    (jt/zoned-date-time 2016 10 2 21 0 0 0 ny-time-zone)
                    (jt/zoned-date-time 2016 10 3 21 0 0 0 ny-time-zone))]
        (is (= (count result) 0))))

    (testing "non-recurring events"
      (let [description {:name "Special promition Oct'16"
                         :start (jt/zoned-date-time 2016 10 5 18 0 0 0 ny-time-zone)
                         :duration (jt/duration 1 :hours)
                         :recurrence :never
                         :scale 2.5
                         :affected-area [:web :mobile]}
            result (expand-description-into-events
                    description
                    (jt/zoned-date-time 2016 10 5 12 0 0 0 utc-time-zone)
                    (jt/zoned-date-time 2016 10 6 12 0 0 0 utc-time-zone))]
        (is (= (count result) 1))
        (let [{begin :begin
               end :end
               scale :scale} (first result)]
          (is (= begin (jt/instant "2016-10-05T22:00:00Z")))
          (is (= end (jt/instant "2016-10-05T23:00:00Z")))
        (is (= scale 2.5)))))))
