(ns cf.kero.string
  (:require [clojure.spec.alpha :as spec]
            [org.clojars.smee.binary.core :as bin]))

(def charset
  "The name of the charset used for encoding Kero Blaster strings."
  "SJIS")

(def string-codec
  "Codec for strings in Kero Blaster files."
  (bin/string charset :prefix :byte))

(defn kero-string-validator
  "Returns a function checking the byte length of a Kero Blaster string.
  `max-length` is the maximum allowed byte length.
  The returned function yields `true` if the length is valid, `false` otherwise."
  [max-length]
  #(<= (count (.getBytes ^String % ^String charset)) max-length))

(def max-name-length
  "The maximum byte length of a name string."
  15)

(spec/def ::name (kero-string-validator max-name-length))
