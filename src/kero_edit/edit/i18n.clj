(ns kero-edit.edit.i18n
  (:require [clojure.edn]
            [clojure.java.io :as io]
            [me.raynes.fs :as fs]
            [taoensso.tempura :as tempura]
            [cljfx.api :as fx])
  (:import [java.io File PushbackReader]))

(def translation-file-regex
  "Regex for locating translation files."
  #"^.+\.clj$")

(defonce
  ^{:doc "A sequence of translation files for Kero Edit."}
  translation-files
  (fs/find-files (io/file "translations") translation-file-regex))

(defonce
  ^{:doc "A sequence of locales Kero Edit has translations for."}
  translated-locales
  (map #(fs/base-name % true) translation-files))

(defonce
  ^{:doc "Map containing all translations for Kero Edit."
    :private true}
  translation-map
  (reduce
   (fn [translations path]
     (let [locale-kw (keyword (fs/base-name path true))]
       (assoc translations
              locale-kw
              (merge
               {:missing (str locale-kw " missing text")}
               (try
                 (read {:eof {}} (PushbackReader. (io/reader path)))
                 (catch Exception _ {}))))))
   {}
   translation-files))

(defn translate
  "Translation function for Kero Edit. Use to retrieve locale-specific strings"
  [locale resource-id & args]
  (apply tempura/tr {:dict translation-map} [locale :en] [resource-id] args))

(defn translate-sub
  "Translation function that uses the in-context locale.
  Can be used with cljfx subscriptions to avoid explicitly specifying the locale."
  [context resource-id & args]
  (apply translate (fx/sub context :locale) resource-id args))

;; TODO Macro to eliminate (fx/sub context translate-sub key) boilerplate
