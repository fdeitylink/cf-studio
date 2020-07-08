(ns cf.kero.field.head
  (:require [cf.kero.field.tile-layer :as tile-layer]
            [cf.kero.string :as kstr]
            [cf.kero.util :as util]
            [clojure.spec.alpha :as spec]
            [org.clojars.smee.binary.core :as bin]))

(def header
  "Marks the start of a PxPack head. This also marks the start of a PxPack file."
  "PXPACK121127a**")

(def max-description-length
  "The maximum byte length of the description string. The string charset is specified by `cf.kero.string/charset`."
  31)

(def num-referenced-fields
  "The number of other fields referenced by a PxPack field."
  4)

(def num-unknown-bytes
  "The number of contiguous unknown bytes in a PxPack head."
  5)

(def scroll-types
  "A vector of the scrolling types of a PxPack tile layer. The index of each element is its byte value in a PxPack head."
  [:normal :three-fourths :half :quarter :eighth :zero :h-three-fourths :h-half :h-quarter :v0-half])

(spec/def ::description (kstr/kero-string-validator max-description-length))
(spec/def ::fields (spec/coll-of ::kstr/name :count num-referenced-fields))
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
(spec/def ::head (spec/keys :req [::description ::fields ::spritesheet ::unknown-bytes ::bg-color ::layer-metadata]))

(def ^:private layer-metadata-codec
  "Codec for a PxPack head's layer metadata chunk."
  (bin/compile-codec
   (bin/ordered-map
    ::tileset kstr/string-codec
    ::visibility-type :byte
    ::scroll-type :ubyte)
   #(update % ::scroll-type (partial (memfn ^clojure.lang.PersistentVector indexOf e) scroll-types))
   #(update % ::scroll-type (partial get scroll-types))))

(def head-codec
  "Codec for a PxPack head."
  (bin/compile-codec
   (bin/ordered-map
    :header (bin/constant (bin/c-string "UTF-8") header)
    ::description kstr/string-codec
    ::fields (bin/repeated kstr/string-codec :length num-referenced-fields)
    ::spritesheet kstr/string-codec
    ::unknown-bytes (bin/repeated :byte :length num-unknown-bytes)
      ;; TODO Push colors into head.bg-color ns? Make them non-namespaced?
    ::bg-color (apply bin/ordered-map (interleave [::red ::green ::blue] (repeat :ubyte)))
      ;; TODO Break out into ::tilesets, ::visibility-types, ::scroll-types
    ::layer-metadata (apply bin/ordered-map (interleave tile-layer/layers (repeat layer-metadata-codec))))
   #(assoc % :header header)
   #(dissoc % :header)))
