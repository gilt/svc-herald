(ns svc-herald.handler
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [java-time :as jt]
            [ring.util.response :as response]
            [ring.util.http-response :refer :all]
            [svc-herald.configuration :as cnf]
            [svc-herald.events :as events]
            [svc-herald.json-codecs :as json]
            [svc-herald.auth :as auth]))

(def project-version
  (-> (io/resource "project.clj") slurp read-string (nth 2)))

(defn api-event-routes [store auth]
  (routes
   (GET "/" []
     (ok (events/all-event-descriptions store)))

   (GET "/:id" [id]
     (let [event (events/get-by-id store id)]
       (if (nil? event)
         (not-found)
         (ok event))))

   (POST "/" {body :body :as request}
     (let [parse-result (json/coerce-to json/EventDescription-withoutId body)]
       (cond
         (schema.utils/error? parse-result) (do
                                              (log/warn "Validation error: " parse-result)
                                              (bad-request))
         :else (ok (events/insert-event-description! store
                                                     parse-result
                                                     (auth/get-current-user-guid auth request))))))

   (PUT "/:id" {{id :id} :params
                body :body :as request}
     (let [parse-result (json/coerce-to json/EventDescription body)]
       (cond
         (schema.utils/error? parse-result) (do
                                              (log/warn "Validation error: " parse-result)
                                              (bad-request))
         :else (do
                 (events/merge-event-description! store
                                                  parse-result
                                                  (auth/get-current-user-guid auth request))
                 (no-content)))))

   (DELETE "/:id" {{id :id} :params :as request}
     (log/info "Deleting event: " id)
     (events/delete-event-description store
                                      id
                                      (auth/get-current-user-guid auth request))
     (no-content))))

(defn app-routes [store
                  auth]
  (routes
   (context "/herald" []
     (wrap-routes
      (routes
       (GET "/whoami" [request]
         (let [user (auth/get-user-info auth
                                        (auth/get-current-user-guid auth request))]
           (ok {:user user})))

       (GET "/" []
         (-> "index.html"
             (response/resource-response {:root "public"})
             (response/content-type "text/html"))))
      #(auth/authenticate auth %))

     (GET "/_internal_/status" []
       (ok {:version project-version}))

     (context "/events" []
       (wrap-routes (api-event-routes store auth)
                    #(auth/authenticate auth % forbidden)))

     (GET "/schedule" []
       (let [begin (jt/minus (jt/zoned-date-time) (jt/hours 12))
             end (jt/plus (jt/zoned-date-time) (jt/hours 12))
             schedule-items (events/schedule-for-period store begin end)]
         (log/info "Getting schedule around now")
         (ok schedule-items))) (route/resources "/webjars" {:root "META-INF/resources/webjars"})
     (route/resources "/"))

   (route/not-found "Not Found")))
