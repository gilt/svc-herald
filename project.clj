(defproject svc-herald "0.8.1-SNAPSHOT"
  :description "Service that keeps schedule of the events and notifies other services about upcoming scaling events"
  :min-lein-version "2.0.0"
  :dependencies [
                 [alandipert/enduro "1.2.0"]
                 [ch.qos.logback/logback-classic "1.1.3"]
                 [clj-http "3.4.1"]
                 [clojure.java-time "0.2.0"]
                 [compojure "1.5.1"]
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [overtone/at-at "1.2.0"]
                 [prismatic/schema "1.1.3"]
                 [ring/ring-core "1.5.0"]
                 [ring/ring-jetty-adapter "1.5.0"]
                 [ring/ring-json "0.4.0"
                  :exclusions [com.fasterxml.jackson.core/jackson-core]] ;;Conflicting with aws-sdk, we want 2.6.6 required by sdk
                 [ring/ring-defaults "0.2.1"]
                 [metosin/ring-http-response "0.8.0"]

                 ;;AWS
                 ;;Since we use only s3 we don't want the whole sdk to be referenced
                 [amazonica "0.3.76" :exclusions [com.amazonaws/aws-java-sdk
                                                  com.amazonaws/amazon-kinesis-client]]
                 [com.amazonaws/aws-java-sdk-core "1.11.52"]
                 [com.amazonaws/aws-java-sdk-s3 "1.11.52"]
                 [com.amazonaws/aws-java-sdk-sns "1.11.52"]

                 ;; UI webjars
                 [org.webjars/requirejs "2.3.2"]
                 [org.webjars/requirejs-text "2.0.15"]
                 [org.webjars/jquery "3.1.1"]
                 [org.webjars/lodash "4.15.0"]
                 [org.webjars/backbonejs "1.3.2"]
                 [org.webjars/backbone.epoxy "1.2"]
                 [org.webjars/bootstrap "3.3.7"]
                 [org.webjars/bootstrap-select "1.9.4"]
                 [org.webjars.bower/momentjs "2.15.2"]
                 [org.webjars/moment-timezone "0.5.5"]
                 [org.webjars.bower/eonasdan-bootstrap-datetimepicker "4.17.43"
                  :exclusions [org.webjars.bower/jquery org.webjars.bower/moment]]
                 [org.webjars.bower/fontawesome "4.6.3"]]

  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                                  [ring/ring-mock "0.3.0"]]}}

  :main svc-herald.main

  :test-selectors {:default (complement :integration)
                   :integration :integration})
