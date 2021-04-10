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
(spec/def ::area-x ::util/ushort)
(spec/def ::area-y ::util/ushort)
(spec/def ::area ::util/ubyte)
(spec/def ::bg-color (spec/and
                      (spec/map-of (constantly true) ::util/ubyte)
                      (spec/keys :req [::red ::green ::blue])))
(spec/def ::tilesets (spec/map-of
                      tile-layer/layers
                      ::kstr/name
                      :count 3))
(spec/def ::visibility-types (spec/map-of
                              tile-layer/layers
                              ::util/byte
                              :count 3))
(spec/def ::scroll-types (spec/map-of
                          tile-layer/layers
                          (set scroll-types)
                          :count 3))
(spec/def ::metadata (spec/keys :req [::name
                                      ::left-field
                                      ::right-field
                                      ::up-field
                                      ::down-field
                                      ::spritesheet
                                      ::area-x
                                      ::area-y
                                      ::area
                                      ::bg-color
                                      ::tilesets
                                      ::visibility-types
                                      ::scroll-types]))

(def ^:private layer-metadata-codec
  "Codec for PxPack metadata's layer metadata chunk."
  (bin/compile-codec
   (bin/ordered-map
    :tileset kstr/string-codec
    :visibility-type :byte
    ::scroll-type :ubyte)
   (fn [lm] (update lm ::scroll-type #(.indexOf ^clojure.lang.PersistentVector scroll-types %)))
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
    ::area-x :ushort-le
    ::area-y :ushort-le
    ::area :ubyte
    ;; TODO Push colors into metadata.bg-color ns? Make them non-namespaced?
    ::bg-color (apply bin/ordered-map (interleave [::red ::green ::blue] (repeat :ubyte)))
    :layer-metadata (apply bin/ordered-map (interleave tile-layer/layers (repeat layer-metadata-codec))))
   (fn [{::keys [tilesets visibility-types scroll-types] :as metadata}]
     (reduce-kv
      (fn [metadata meta-kw meta-map]
        (reduce-kv
         (fn [metadata layer value]
           (assoc-in metadata [:layer-metadata layer meta-kw] value))
         metadata
         meta-map))
      (-> metadata
         (dissoc ::tilesets ::visibility-types ::scroll-types)
         (assoc :header header))
      {:tileset tilesets :visibility-type visibility-types :scroll-type scroll-types}))
   (fn [{:keys [layer-metadata] :as metadata}]
     (reduce-kv
      (fn [metadata layer {:keys [tileset visibility-type scroll-type]}]
        (-> metadata
            (assoc-in [::tilesets layer] tileset)
            (assoc-in [::visibility-types layer] visibility-type)
            (assoc-in [::scroll-types layer] scroll-type)))
      (dissoc metadata :header :layer-metadata)
      layer-metadata))))
