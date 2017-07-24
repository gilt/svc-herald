(ns svc-herald.handler-test
  (:require [cheshire.core :as json]
            [clojure.test :refer :all]
            [java-time :as jt]
            [ring.mock.request :as mock]
            [ring.util.http-response :as http]
            [svc-herald.main :refer [create-app create-store]]
            [svc-herald.audit :as audit]
            [svc-herald.auth :as auth]
            [svc-herald.events :as events]
            [svc-herald.utils :refer [ny-time-zone]]))

(defn herald-url [path]
  (str "/herald" path))

(def test-auth (auth/no-auth))

(deftest events-api-test
  (testing "insert(PUT)-list-delete scenario"
    (let [store (create-store {:type :in-mem})
          test-app (create-app store
                               test-auth)
          id (java.util.UUID/randomUUID)
          event-description (events/event-description id
                                                      "test"
                                                      (jt/zoned-date-time 2016 10 1 12 0 0 0 ny-time-zone)
                                                      (jt/duration 1 :hours)
                                                      :daily
                                                      1
                                                      [:web :mobile])]

      (testing "put 1 item"
        (let [request (->
                       (mock/request :put
                                     (herald-url (format "/events/%s" id))
                                     (json/generate-string event-description))
                       (mock/content-type "application/json"))
              response (test-app request)]
          (is (= (:status response) 204))))

      (testing "there should be exactly 1 item"
        (let [request (-> (mock/request :get (herald-url "/events")))
              response (test-app request)
              deser-response (json/parse-string (:body response) true)]
          (is (= (:status response) 200))
          (is (= (count deser-response) 1))))

      (testing "getting that item by id"
        (let [request (-> (mock/request :get (herald-url (format "/events/%s" id))))
              response (test-app request)
              deser-response (json/parse-string (:body response) true)]
          (is (= (:status response) 200))
          (is (= (:name deser-response) "test"))
          (is (= (:id deser-response) (.toString id)))))

      (testing "delete create item"
        (let [request (-> (mock/request :delete (herald-url (format "/events/%s" id))))
              response (test-app request)]
          (is (= (:status response) 204))))

      (testing "check there in nothing left"
        (let [request (-> (mock/request :get (herald-url "/events")))
              response (test-app request)
              deser-response (json/parse-string (:body response))]
          (is (= (:status response) 200))
          (is (count deser-response) 0)))))

  (testing "trying to submit malformed json scenario to PUT endpoint"
    (let [store (create-store {:type :in-mem})
          test-app (create-app store
                               test-auth)
          id (java.util.UUID/randomUUID)
          event-description {:id "65639699-eae3-4fa0-82f3-a02c903bb0bf"
                             :name "test malformed json"
                             :start "2016-10-01T12:00-04:00[America/New_York]"
                             :duration "PT1H"
                             :recurrence "daily"
                             :scale 1
                             :affected ["web", "mobile"]
                             :active true}]
      (testing "Prove that initial record is valid"
        (let [request (->
                       (mock/request :put
                                     (herald-url (format "/events/%s" id))
                                     (json/generate-string event-description))
                       (mock/content-type "application/json"))
              response (test-app request)]
          (is (= (:status response) 204))))

      (testing "Absence of the field"
        (let [bad-data (dissoc event-description :active)
              request (->
                       (mock/request :put
                                     (herald-url (format "/events/%s" id))
                                     (json/generate-string bad-data))
                       (mock/content-type "application/json"))
              response (test-app request)]
          (is (= (:status response) 400))))

      (testing "Malformed datetime"
        (let [bad-data (assoc event-description :start "22-10-01T12:00[America/New_York]")
              request (->
                       (mock/request :put
                                     (herald-url (format "/events/%s" id))
                                     (json/generate-string bad-data))
                       (mock/content-type "application/json"))
              response (test-app request)]
          (is (= (:status response) 400))))))

  (testing "POST events endpoint"
    (let [store (create-store {:type :in-mem})
          test-app (create-app store
                               test-auth)
          event-description {:name "test post endpoint"
                             :start "2016-10-01T12:00-04:00[America/New_York]"
                             :duration "PT1H"
                             :recurrence "daily"
                             :scale 1
                             :affected ["web", "mobile"]
                             :active true}]
      (testing "Insertion works when valid data submitted"
        (let [request (->
                       (mock/request :post
                                     (herald-url "/events/")
                                     (json/generate-string event-description))
                       (mock/content-type "application/json"))
              response (test-app request)
              deser-response (json/parse-string (:body response) true)]
          (is (= (:status response) 200))
          (is (= (count (events/all-active-event-descriptions store)) 1))
          (is (re-matches #"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$" (:id deser-response)))
          (is (= (:name event-description) (:name deser-response)))))

      (testing "Fails when data is malformed"
        (let [bad-data (assoc event-description :duration "asjhfgaskfhjadgsf")
              request (->
                       (mock/request :post
                                     (herald-url "/events/")
                                     (json/generate-string bad-data)))
              response (test-app request)]
          (is (= (:status response) 400))))))

  (testing "not-found route"
    (let [store (create-store {:type :in-mem})
          test-app (create-app store
                               test-auth)
          response (test-app (-> (mock/request :get (herald-url "/invalid"))))]
      (is (= (:status response) 404)))))
