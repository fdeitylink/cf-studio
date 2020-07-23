(ns cf.kero.field.metadata
  (:require [cf.kero.field.tile-layer :as tile-layer]
            [cf.kero.string :as kstr]
            [cf.util :as util]
            [clojure.spec.alpha :as spec]
            [org.clojars.smee.binary.core :as bin]))

(def header
  "Marks the start of PxPack metadata. This also marks the start of a PxPack file."
  "PXPACK121127a**")

(def max-name-length
  "The maximum byte length of the name. The string charset is specified by `cf.kero.string/charset`."
  31)

(def num-unknown-bytes
  "The number of contiguous unknown bytes in PxPack metadata."
  5)

(def scroll-types
  "A vector of the scrolling types of a PxPack tile layer. The index of each element is its byte value in PxPack metadata."
  [:normal
   :three-fourths
   :half
   :quarter
   :eighth
   :zero
   :h-three-fourths
   :h-half
   :h-quarter
   :v0-half])

(spec/def ::name (kstr/kero-string-validator max-name-length))
(spec/def ::left-field ::kstr/name)
(spec/def ::right-field ::kstr/name)
(spec/def ::up-field ::kstr/name)
(spec/def ::down-field ::kstr/name)
(spec/def ::spritesheet ::kstr/name)
(spec/def ::unknown-bytes (spec/coll-of ::util/byte :count num-unknown-bytes))
(spec/def ::bg-color (spec/and
                      (spec/map-of (constantly true) ::util/ubyte)
                      (spec/keys :req [::red ::green ::blue])))
(spec/def ::tileset ::kstr/name)
(spec/def ::visibility-type ::util/byte)
(spec/def ::scroll-type #(some #{%} scroll-types))
(spec/def ::layer-metadata (spec/and
                            (spec/map-of (constantly true) (spec/keys :req [::tileset ::visibility-type ::scroll-type]))
                            (spec/keys :req [::tile-layer/foreground ::tile-layer/middleground ::tile-layer/background])))
(spec/def ::metadata (spec/keys :req [::name
                                      ::left-field
                                      ::right-field
                                      ::up-field
                                      ::down-field
                                      ::spritesheet
                                      ::unknown-bytes
                                      ::bg-color
                                      ::layer-metadata]))

(def ^:private layer-metadata-codec
  "Codec for PxPack metadata's layer metadata chunk."
  (bin/compile-codec
   (bin/ordered-map
    ::tileset kstr/string-codec
    ::visibility-type :byte
    ::scroll-type :ubyte)
   #(update % ::scroll-type (partial (memfn ^clojure.lang.PersistentVector indexOf e) scroll-types))
   #(update % ::scroll-type (partial get scroll-types))))

(def metadata-codec
  "Codec for PxPack metadata."
  (bin/compile-codec
   (bin/ordered-map
    :header (bin/constant (bin/c-string "UTF-8") header)
    ::name kstr/string-codec
    ::left-field kstr/string-codec
    ::right-field kstr/string-codec
    ::up-field kstr/string-codec
    ::down-field kstr/string-codec
    ::spritesheet kstr/string-codec
    ::unknown-bytes (bin/repeated :byte :length num-unknown-bytes)
      ;; TODO Push colors into metadata.bg-color ns? Make them non-namespaced?
    ::bg-color (apply bin/ordered-map (interleave [::red ::green ::blue] (repeat :ubyte)))
      ;; TODO Break out into ::tilesets, ::visibility-types, ::scroll-types
    ::layer-metadata (apply bin/ordered-map (interleave tile-layer/layers (repeat layer-metadata-codec))))
   #(assoc % :header header)
   #(dissoc % :header)))
