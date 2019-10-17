(ns kero-edit.edit.i18n
  (:require [clojure.edn]
            [clojure.java.io :as io]
            [me.raynes.fs :as fs]
            [tongue.core :as tongue]
            [cljfx.api :as fx]))

(defonce
  ^{:doc "A sequence of locales Kero Edit has translations for."}
  translated-locales
  (map #(fs/base-name % true)
       (fs/find-files (io/resource "translations") (re-pattern "^.+\\.edn$"))))

(defonce
  ^{:doc "Map containing all translations for Kero Edit."
    :private true}
  translation-map
  (apply merge
         {:tongue/fallback :en}
         (map (fn [path]
                (array-map
                 (keyword (fs/base-name path true))
                 (try
                   (clojure.edn/read {:eof {}} (java.io.PushbackReader. (io/reader path)))
                   (catch Exception _ {}))))
              (fs/find-files (io/resource "translations") (re-pattern "^.+\\.edn$")))))

(defonce
  ^{:doc "Translation function for Kero Edit. Use to retrieve locale-specific strings."}
  translate
  (tongue/build-translate translation-map))

(defn sub-translate
  "Translation function that uses the in-context locale.
  Can be used with cljfx subscriptions to avoid explicitly specifying the locale."
  [context key & args]
  (apply translate (fx/sub context ::locale) key args))

;; TODO Macro to eliminate (fx/sub context sub-translate key) boilerplate
