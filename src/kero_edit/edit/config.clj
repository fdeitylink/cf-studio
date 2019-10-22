(ns kero-edit.edit.config
  (:require [clojure.edn]
            [clojure.pprint :refer [pprint]]
            [clojure.java.io :as io]
            [me.raynes.fs :as fs]
            [kero-edit.edit.i18n :as i18n])
  (:import [java.io PushbackReader]))

(def default-config-path
  "Default file path for the Kero Edit configuration file."
  "./config.edn")

(def default-config
  "Default configuration map for Kero Edit."
  {:license-accepted false

   ;; Default is not actually nil, just locale-specific (look at read-config)
   :notepad-text nil

   :locale :en

   :last-executable-path nil})

(defn read-config
  "Reads in a Kero Edit configuration file.
  `config-path` is the path to the configuration file. This should be an edn file with a map namespaced under `kero-edit.edit.app`. If `config-path` is not given or is invalid, a config file is looked for at `default-config-path`."
  ([] (read-config default-config-path))
  ([config-path]
   (let [path (if (fs/file? config-path) config-path default-config-path)
         user-config (merge
                      default-config
                      (try
                        (clojure.edn/read {:eof {}} (PushbackReader. (io/reader path)))
                        (catch Exception _ {})))
         {:keys [notepad-text locale]} user-config]
     (if (some? notepad-text)
       user-config
       (assoc user-config :notepad-text (i18n/translate locale ::default-notepad-text))))))

(defn write-config
  "Writes out a Kero Edit configuration file.
  `config-path` is the path to the configuration file. If not provided, `default-config-path` is used.
  `config` is the actual configuration map."
  ([config] (write-config default-config-path config))
  ([config-path config]
   (pprint config (io/writer config-path))))
