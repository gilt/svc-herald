(ns svc-herald.json-codecs
  (:require [cheshire.generate :refer [add-encoder]]
            [cheshire.core :refer [generate-string, parse-string]]
            [schema.core :as s]
            [schema.coerce :as coerce]))

(add-encoder java.time.ZonedDateTime
             (fn [c jsonGenerator]
               (.writeString jsonGenerator (str c))))

(add-encoder java.time.Duration
             (fn [c jsonGenerator]
               (.writeString jsonGenerator (str c))))

(add-encoder java.time.Instant
             (fn [c jsonGenerator]
               (.writeString jsonGenerator (str c))))

(defn to-json-str [value]
  (generate-string value))

(defn from-json-str [string]
  (parse-string string true))

;; Coercing-related things

(defn- zoned-date-time-matcher [schema]
  (when (= java.time.ZonedDateTime schema)
    (coerce/safe #(java.time.ZonedDateTime/parse ^String %))))

(defn- duration-matcher [schema]
  (when (= java.time.Duration schema)
    (coerce/safe #(java.time.Duration/parse ^String %))))

;; Coercers

(def matchers (coerce/first-matcher [zoned-date-time-matcher
                                     duration-matcher
                                     coerce/json-coercion-matcher]))

(def EventDescription
  (coerce/coercer
   {:id s/Uuid
    :name s/Str
    :start java.time.ZonedDateTime
    :duration java.time.Duration
    :recurrence (s/enum :weekly :daily :none)
    :affected [(s/enum :web :mobile)]
    :scale s/Num
    :active s/Bool}
   matchers))

(def EventDescription-withoutId
  (coerce/coercer
   {:name s/Str
    :start java.time.ZonedDateTime
    :duration java.time.Duration
    :recurrence (s/enum :weekly :daily :none)
    :affected [(s/enum :web :mobile)]
    :scale s/Num
    :active s/Bool}
   matchers))

(defn coerce-to [coercer data]
  (coercer data))
