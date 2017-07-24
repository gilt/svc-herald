(ns svc-herald.audit
  (:require [alandipert.enduro :as enduro]
            [java-time :as jt]
            [clojure.tools.logging :as log]
            [svc-herald.utils :as utils]))

(defn audit-entry [action entity-id user-id]
  {:user (str user-id)
   :action action
   :id (str entity-id)
   :timestamp (jt/zoned-date-time utils/utc-time-zone)})
