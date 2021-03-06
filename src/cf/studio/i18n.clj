(ns cf.studio.i18n
  (:require [clojure.java.io :as io]
            [me.raynes.fs :as fs]
            [taoensso.tempura :as tempura])
  (:import java.io.PushbackReader))

(def ^:private translation-file-regex
  "Regex for locating translation files."
  #"^.+\.clj$")

(defonce
  ^{:doc "Sequence of translation files for Cat & Frog Studio."
    :private true}
  translation-files
  (fs/find-files (io/file "translations") translation-file-regex))

(defonce
  ^{:doc "Sequence of locales Cat & Frog Studio has translations for."}
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
  "Translation function for Cat & Frog Studio. Use to retrieve locale-specific strings."
  [locale resource-id & args]
  (apply tempura/tr {:dict translation-map} [locale :en] [resource-id] args))

;; TODO tempura uses its own internal caching; make this fx/sub-val'able
(defn translate-sub
  "Translation function that uses the in-context locale.
  Can be used with [[cljfx.api/sub-val]] to avoid explicitly specifying the locale."
  [context-val resource-id & args]
  (apply translate (:locale context-val) resource-id args))
