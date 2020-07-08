(ns cf.studio.i18n
  (:require [cljfx.api :as fx]
            [clojure.java.io :as io]
            [me.raynes.fs :as fs]
            [taoensso.tempura :as tempura])
  (:import java.io.PushbackReader))

(def translation-file-regex
  "Regex for locating translation files."
  #"^.+\.clj$")

(defonce
  ^{:doc "A sequence of translation files for Cat & Frog Studio."
    :private true}
  translation-files
  (fs/find-files (io/file "translations") translation-file-regex))

(defonce
  ^{:doc "A sequence of locales Cat & Frog Studio has translations for."}
  translated-locales
  (map #(fs/base-name % true) translation-files))

(defonce
  ^{:doc "Map containing all translations for Cat & Frog Studio."
    :private true}
  translation-map
  (reduce
   (fn [translations path]
     (let [locale-kw (keyword (fs/base-name path true))]
       (assoc translations locale-kw
              (merge {:missing (str locale-kw " missing text")}
                     (try
                       (read {:eof {}} (PushbackReader. (io/reader path)))
                       (catch Exception _ {}))))))
   {}
   translation-files))

(defn translate
  "Translation function for Cat & Frog Studio. Use to retrieve locale-specific strings"
  [locale resource-id & args]
  (apply tempura/tr {:dict translation-map} [locale :en] [resource-id] args))

(defn translate-sub
  "Translation function that uses the in-context locale.
  Can be used with cljfx subscriptions to avoid explicitly specifying the locale."
  [context resource-id & args]
  (apply translate (fx/sub context :locale) resource-id args))

;; TODO Macro to eliminate (fx/sub context translate-sub key) boilerplate
