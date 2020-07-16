(ns cf.studio.config
  (:require clojure.edn
            [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [me.raynes.fs :as fs])
  (:import java.io.PushbackReader))

(def default-config-path
  "Default file path for the Cat & Frog Studio configuration file."
  "./config.edn")

(def default-config
  "Default configuration map for Cat & Frog Studio."
  {:license-accepted false
   :locale :en
   :last-executable-path nil})

(defn read-config
  "Reads in a Cat & Frog Studio configuration file.
  Returns map of `:config`, the configuration map, and `:config-path`, the path ultimately used for the config file.
  `config-path` is the path to the configuration file. This should be an edn file with a map. If `config-path` is not given or is invalid, a config file is looked for at `default-config-path`."
  ([] (read-config default-config-path))
  ([config-path]
   (let [path (if (fs/file? config-path) config-path default-config-path)]
     {:config-path path
      :config (merge
               default-config
               (try
                 (clojure.edn/read {:eof {}} (PushbackReader. (io/reader path)))
                 (catch Exception _ {})))})))

(defn write-config
  "Writes out a Cat & Frog Studio configuration file.
  `config-path` is the path to the configuration file. If not provided, `default-config-path` is used.
  `config` is the actual configuration map."
  ([config] (write-config default-config-path config))
  ([config-path config]
   (pprint config (io/writer config-path))))
