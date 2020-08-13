(ns cf.studio.config
  (:require [cljfx.api :as fx]
            clojure.edn
            [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [me.raynes.fs :as fs])
  (:import java.io.PushbackReader))

(def default-config-path
  "Default file path for the Cat & Frog Studio config file."
  "./config.edn")

(def default-config
  "Default config map for Cat & Frog Studio."
  {:license-accepted false
   :locale :en
   :last-executable-path nil})

(defn context->config
  "Returns the submap of `context` containing the user config."
  [context]
  (select-keys (fx/sub context) (cons :config-path (keys default-config))))

(defn read-config!
  "Reads in a Cat & Frog Studio config file.
  `config-path` is the path to the config file. This should be an edn file with a map.
  If `config-path` is invalid, `default-config-path` is used."
  [config-path]
  (let [path (if (fs/file? config-path) config-path default-config-path)]
    (merge
     {:config-path path}
     default-config
     (try
       (clojure.edn/read {:eof {}} (PushbackReader. (io/reader path)))
       (catch Exception _ {})))))

(defn write-config!
  "Writes out a Cat & Frog Studio config file.
  `config` is the user config map."
  [{:keys [config-path] :as config}]
  (pprint (dissoc config :config-path) (io/writer config-path)))
