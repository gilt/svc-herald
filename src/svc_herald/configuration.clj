(ns svc-herald.configuration
  (:require [ring.util.http-response]
            [clojure.string :as str]
            [clojure.java.shell]))

(def default-conf {:retries {:attempts 3
                             :delay 1000
                             :scatter 500}

                   :store {:type :in-mem}

                   :port (Integer/parseInt (or (System/getenv "PORT")
                                               "3000"))

                   :notifications {:type :log
                                   :period (* 60 1000)}})

(def sns-s3-conf {:retries {:attempts 3
                            :delay 1000
                            :scatter 500}

                  :store {:type :s3
                          :bucketname "herald-bucket"
                          :filename "schedule.json"}

                  :port (Integer/parseInt (or (System/getenv "PORT")
                                              "3000"))

                  :notifications {:type :sns
                                  :period (* 15 60 1000)
                                  :sns-topic-arn "arn:aws:sns:[region]:[account]:[sns-topic-name]"
                                  :sns-message-subject "Recent and upcoming"}})

(def current-config (delay default-conf))
