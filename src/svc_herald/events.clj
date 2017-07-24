(ns svc-herald.events
  (:require [alandipert.enduro :as enduro]
            [java-time :as jt]
            [svc-herald.audit :as audit]
            [svc-herald.utils :as utils]))

(defn all-event-descriptions [store]
  (sequence (vals (:events @store))))

(defn get-by-id [store id]
  (get-in @store [:events id]))

(defn merge-event-description! [store event-description current-user-guid]
  (let [id (str (:id event-description))
        action (if (some? (get-in @store [:events id]))
                          :update-event
                          :insert-event)]
    (enduro/swap! store
                  (fn [current]
                    (-> current
                        (assoc-in [:events id]
                                  event-description)
                        (update-in [:audit]
                                   conj
                                   (audit/audit-entry action id current-user-guid)))))))

(defn insert-event-description! [store event-description-without-id current-user-guid]
  (let [event-description (assoc event-description-without-id :id (java.util.UUID/randomUUID))]
    (merge-event-description! store event-description current-user-guid)
    event-description))

(defn delete-event-description [store id current-user-guid]
  (enduro/swap! store
                (fn [current]
                  (-> current
                      (update-in [:events]
                                 dissoc
                                 id)
                      (update-in [:audit]
                                 conj
                                 (audit/audit-entry :delete-event
                                                    id
                                                    current-user-guid))))))

(defn all-active-event-descriptions [store]
  (filter (fn [x] (:active x)) (all-event-descriptions store)))


(defn event-description [id name start duration recurrence scale affected]
  {:id id
   :name name
   :start start
   :duration duration
   :recurrence recurrence
   :scale scale
   :affected affected
   :active true})

(defn- closest-occurence-before [start recurrence point-now]
  (let [seconds-between (jt/time-between start point-now :seconds)
        occurences-count (quot seconds-between
                               (jt/as recurrence :seconds))]
    (if (neg? seconds-between)
      nil
      (jt/plus start (jt/multiply-by recurrence occurences-count)))))

(defn- ranges-intersects? [[begin1 end1]
                          [begin2 end2]]
  (cond
    (jt/before? end1 begin2) false
    (jt/after? begin1 end2) false
    :else true))

(defn- create-event [description start]
  {:name (:name description)
   :begin (jt/instant start)
   :end (jt/plus (jt/instant start)
                 (:duration description))
   :scale (:scale description)
   :affected (:affected description)})

(defn- expand-recurring-description [description begin end]
  (let [event-recurrence (cond
                           (= :daily (:recurrence description)) (jt/days 1)
                           (= :weekly (:recurrence description)) (jt/weeks 1))
        event-start (:start description)
        sample-before-range (or (closest-occurence-before event-start event-recurrence begin)
                                event-start)
        event-starts-in-range (loop [sample sample-before-range
                                     events-found []]
                                (if (jt/after? sample end)
                                  events-found
                                  (recur (jt/plus sample event-recurrence)
                                         (if (ranges-intersects? [begin end]
                                                                 [sample (jt/plus sample (:duration description))])
                                           (conj events-found sample)
                                           events-found))))]
    (map (fn [e] (create-event description e)) event-starts-in-range)))

(defn- expand-nonrecurring-description [description begin end]
  (let [event-start (:start description)]
    (if (ranges-intersects? [begin end]
                            [event-start (jt/plus event-start (:duration description))])
      [(create-event description event-start)])))

(defn expand-description-into-events [description begin end]
  (if (= (:recurrence description) :never)
    (expand-nonrecurring-description description begin end)
    (expand-recurring-description description begin end)))

(defn schedule-for-period [events-store begin end]
  (mapcat
   (fn [d] (expand-description-into-events d begin end))
   (all-active-event-descriptions events-store)))
