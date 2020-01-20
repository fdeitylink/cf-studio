(defproject kero-edit "0.1.0-SNAPSHOT"
  :description "A modding tool for Kero Blaster"
  :url "https://github.com/fdeitylink/kero-edit"
  :license {:name "Apache License 2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [smee/binary "0.5.4"]
                 [org.flatland/ordered "1.5.7"]
                 [clj-commons/fs "1.5.1"]
                 [cljfx "1.6.1"]
                 [com.taoensso/tempura "1.2.1"]]
  :main ^:skip-aot kero-edit.edit.app
  :target-path "target/%s"
  :profiles {:uberjar
             {:aot :all
              ;; Build starts up FX Platform, need to close it when finished
              :injections [(javafx.application.Platform/exit)]}})
