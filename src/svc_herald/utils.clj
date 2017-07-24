(ns svc-herald.utils
  (:require [clojure.tools.logging :as log]
            [java-time :as jt]
            [svc-herald.configuration :as cnf]))

(defn random-uuid []
  (java.util.UUID/randomUUID))

(def ny-time-zone
  (jt/zone-id "America/New_York"))

(def utc-time-zone
  (jt/zone-id "UTC"))

(defn- wait [retry-config]
  (let [min-delay (:delay retry-config)
        addtitional-delay (:scatter retry-config)
        scatter (rand-int (inc addtitional-delay))]
    (Thread/sleep (+ min-delay scatter))))

(defn retry
  ([retry-config action]
   (loop [attempts-left (:attempts retry-config)]
     (if-let [result (try
                       [(action)]
                       (catch Exception e
                         (if (zero? attempts-left)
                           (throw e)
                           (do
                             (wait retry-config)
                             (log/info "Got an exception, retrying..." (str e))))))]
       (result 0)
       (recur (dec attempts-left)))))
  ([action] (retry (:retries @cnf/current-config) action)))

(defn swallow-exception
  [action on-exception]
  (try
    (action)
    (catch Exception e
      (on-exception e))))

(defn with-cookie [response key value]
  (assoc-in response [:cookies key :value] value))
