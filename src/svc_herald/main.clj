(ns svc-herald.main
  (:gen-class)
  (:require [alandipert.enduro :as enduro]
            [amazonica.aws.sns :as sns]
            [cheshire.core :as json]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [java-time :as jt]
            [overtone.at-at :as at-at]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.content-type :refer :all]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults site-defaults]]
            [ring.middleware.json :as middleware]
            [ring.middleware.resource :refer :all]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [svc-herald.auth :as auth]
            [svc-herald.configuration :as cnf]
            [svc-herald.events :as events]
            [svc-herald.handler :as handlers]
            [svc-herald.s3-backed-atom :refer [s3-atom]]
            [svc-herald.utils :as utils]))

(defn- log-request [app]
  (fn [req]
    (let [method (:request-method req)
          uri (:uri req)
          user-guid (:user-guid req)
          response (app req)
          status (get response :status 404)
          log-fn (if (>= status 500) #(log/error %) #(log/info %))]
      (log-fn [status method (str user-guid) uri])
      response)))

(defn create-store
  ([store-config]
   (let [type (:type store-config)]
     (cond
       (= :in-mem type) (enduro/mem-atom {:events {}
                                          :audit []})
       (= :s3 type) (s3-atom {:events {}
                              :audit []}
                             (:bucketname store-config)
                             (:filename store-config))
       :else (throw (Exception. (format "Storage type %s is not supported. Check configuration, only :in-mem and :s3 are supported" type)))))))

(defn get-notification-publisher
  ([notification-config]
   (let [type (:type notification-config)]
     (cond
       (= :log type) (fn [data] (log/info "Publising notification containing: " data))
       (= :sns type) (fn [data]
                       (utils/retry
                        #(sns/publish :topic-arn (:sns-topic-arn notification-config)
                                      :subject (:sns-message-subject notification-config)
                                      :message data)))
       :else (throw (Exception. (format "Messaging type %s is not supported. Check configuration, only :sns and :log types are supported" type)))))))

(defn create-app [store
                  auth]
  (letfn [(wrap-auth-cookie [app] (auth/wrap-auth-cookie auth app))]
    (-> (handlers/app-routes store
                             auth)
        (middleware/wrap-json-body {:keywords? true})
        middleware/wrap-json-response
        wrap-auth-cookie
        wrap-cookies
        (wrap-defaults api-defaults)
        wrap-content-type
        log-request)))

(def window-length-hours 24)

(defn- publish-notification
  "Get unfolded events around <now> and publish sns message containing that data"
  [events-store publisher]
  (let [now (jt/zoned-date-time)
        delta (jt/hours (/ window-length-hours 2))
        begin (jt/minus now delta)
        end (jt/plus now delta)
        schedule (events/schedule-for-period events-store begin end)]
    (log/info (format "Publishing notification with schedule containing %s items" (count schedule)))
    (publisher (json/generate-string schedule))))

(defn -main [& args]
  (let [config @cnf/current-config
        store (create-store (:store @cnf/current-config))
        publisher (get-notification-publisher (:notifications @cnf/current-config))
        notification-period (get-in @cnf/current-config [:notifications :period])
        authenticator (auth/no-auth)
        app (create-app store
                        authenticator)
        thread-pool (at-at/mk-pool)]
    (at-at/every notification-period
                 #(utils/swallow-exception
                   (fn [] (publish-notification store publisher))
                   (fn [ex] (log/error "Failed to send SNS notification" (str ex))))
                 thread-pool)
    (run-jetty app {:port (config :port)})))
