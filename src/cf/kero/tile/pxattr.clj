(ns cf.kero.tile.pxattr
  (:require [cf.util :as util]
            [clojure.spec.alpha :as spec]
            [org.clojars.smee.binary.core :as bin]))

(def header
  "Marks the start of a PxAttr file."
  "pxMAP01")

(def width
  "The width of all PxAttr sets."
  16)

(def height
  "The height of all PxAttr sets."
  16)

(defn get-attribute
  "Returns the tile attribute in a PxAttr at the given coordinates."
  [pxattr x y]
  (get-in pxattr [y x]))

(defn assoc-attribute
  "Associates a tile attribute to a PxAttr at the given coordinates."
  [pxattr x y attribute]
  (assoc-in pxattr [y x] attribute))

(spec/def ::pxattr (spec/coll-of
                    (spec/coll-of ::util/ubyte :count width :kind vector?)
                    :count height
                    :kind vector?))

(def pxattr-codec
  "Codec for PxAttr files."
  (bin/compile-codec
   [(bin/constant (bin/c-string "UTF-8") header)
    (bin/constant :ushort-le width)
    (bin/constant :ushort-le height)
    (bin/constant :byte 0)
    (bin/repeated (bin/repeated :ubyte :length width) :length height)]
   ;; Convert attributes into file data
   (fn [pxattr] [header width height 0 pxattr])
   ;; Turn file data into 2D vector
   (comp (partial mapv vec) #(nth % 4))))
