(ns svc-herald.s3-backed-atom
  (:require [alandipert.enduro :as end]
            [amazonica.aws.s3 :as s3]
            [svc-herald.json-codecs :as json]
            [svc-herald.configuration :as cnf]
            [svc-herald.utils :refer [retry]]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.tools.logging :as log]))

(defn- load-value [bucket key]
  (log/info (format "Loading from S3 %s:%s" bucket key))
  (->> (retry #(s3/get-object bucket key))
       :input-stream
       slurp
       json/from-json-str
       seq
       (map (fn [[key value]]
              (let [object (json/coerce-to json/EventDescription value)]
                [(str (:id object)) object])))
       (into {})))

(defn- store-value [bucket key value]
  (let [js-value (json/to-json-str value)
        bytes (.getBytes js-value)]
    (log/info (format "Storing to S3 %s:%s" bucket key))
    (retry #(s3/put-object :bucket-name bucket
                           :key key
                           :input-stream (io/input-stream bytes)
                           :metadata {:content-length (count bytes)}))))

(deftype S3Backend [bucket key]
  end/IDurableBackend
  (-commit! [this value]
    (store-value bucket key value))
  (-remove! [this]
    (retry #(s3/delete-object bucket key))))

(defn s3-atom
  #=(end/with-options-doc "Creates and returns a S3-backed atom.
  If the location denoted by the combination of bucket and key exists,
  it is read and becomes the initial value.
  Otherwise, the initial value is init and the bucket denoted by table-name is updated.")
  [init bucket filename]
  (end/atom* (if (retry #(s3/does-object-exist bucket filename))
               (do
                 (log/info "Restoring state from S3")
                 (load-value bucket filename))
               init)
             (S3Backend. bucket filename)
             {}))
