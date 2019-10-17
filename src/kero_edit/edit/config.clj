(ns kero-edit.edit.config
  (:require [clojure.edn]
            [clojure.pprint :refer [pprint]]
            [clojure.java.io :as io]
            [me.raynes.fs :as fs]))

(def default-config-path
  "Default file path for the Kero Edit configuration file."
  "./config.edn")

(def default-config
  "Default configuration map for Kero Edit."
  #:kero-edit.edit.app{})

(defn read-config
  "Reads in a Kero Edit configuration file.
  `config-path` is the path to the configuration file. This should be an edn file with a map namespaced under `kero-edit.edit.app`. If `config-path` is not given or is invalid, a config file is looked for at `default-config-path`."
  ([] (read-config default-config-path))
  ([config-path]
   (let [path (if (fs/file? config-path) config-path default-config-path)]
     (merge
      default-config
      (try
        (clojure.edn/read {:eof {}} (java.io.PushbackReader. (io/reader path)))
        (catch Exception _ {}))))))

(defn write-config
  "Writes out a Kero Edit configuration file.
  `config-path` is the path to the configuration file. If not provided, `default-config-path` is used.
  `config` is the actual configuration map."
  ([] (write-config default-config-path))
  ([config-path config]
   (pprint config (io/writer config-path))))
