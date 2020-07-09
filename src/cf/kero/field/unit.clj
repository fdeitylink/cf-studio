(ns cf.kero.field.unit
  (:require [cf.kero.string :as kstr]
            [cf.util :as util]
            [clojure.spec.alpha :as spec]
            [org.clojars.smee.binary.core :as bin]))

(def max-type-index
  "The maximum value for a PxPack unit's type."
  0xFF)
  ;174)

(def num-unknown-bytes
  "The number of unknown bytes in a PxPack unit."
  4)

(def max-coordinate-index
  "A PxPack unit's maximum x or y coordinate. Equivalent to the maximum of an unsigned short minus one."
  0xFFFF)
  ;0xFFFE)

(spec/def ::type (spec/int-in 0 (inc max-type-index)))
(spec/def ::x (spec/int-in 0 (inc max-coordinate-index)))
(spec/def ::y (spec/int-in 0 (inc max-coordinate-index)))
(spec/def ::name ::kstr/name)
(spec/def ::unknown-bytes (spec/coll-of ::util/byte :count num-unknown-bytes))
(spec/def ::unit (spec/keys :req [::type ::x ::y ::name ::unknown-bytes]))

(def unit-codec
  "Codec for a PxPack unit."
  (bin/compile-codec
   (bin/ordered-map
    ;; 'ub' means 'unknown byte'
    :ub1 :byte
    ::type :ubyte
    :ub2 :byte
    ::x :ushort-le
    ::y :ushort-le
    :ub3 :byte
    :ub4 :byte
    ::name kstr/string-codec)
   (fn [{[ub1 ub2 ub3 ub4] ::unknown-bytes :as unit}]
     (-> unit
         (dissoc ::unknown-bytes)
         (assoc :ub1 ub1 :ub2 ub2 :ub3 ub3 :ub4 ub4)))
   (fn [{:keys [ub1 ub2 ub3 ub4] :as unit}]
     (-> unit
         (dissoc :ub1 :ub2 :ub3 :ub4)
         (assoc ::unknown-bytes [ub1 ub2 ub3 ub4])))))
