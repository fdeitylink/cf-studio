(ns cf.kero.field.tile-layer
  (:require [cf.util :as util]
            [clojure.spec.alpha :as spec]
            [flatland.ordered.set :refer [ordered-set]]
            [org.clojars.smee.binary.core :as bin]))

(def header
  "Marks the start of a PxPack tile layer."
  "pxMAP01")

(def num-tile-layers
  "The number of tile layers in a PxPack file."
  3)

(def max-tile-index
  "The maximum value for a tile index. Equivalent to the maximum of an unsigned byte."
  0xFF)

(def max-width
  "The maximum width of a tile layer."
  640)

(def max-height
  "The maximum height of a tile layer."
  256)

(def layers
  "An ordered set of keywords representing the three tile layers. The order is that in which tile layers appear in PxPack files."
  (ordered-set ::foreground ::middleground ::background))

(defn width
  "Returns the width of a tile layer."
  [tile-layer]
  (count (first tile-layer)))

(defn height
  "Returns the height of a tile layer."
  [tile-layer]
  (count tile-layer))

(defn get-tile
  "Returns the tile in a tile layer at the given coordinates."
  [tile-layer x y]
  (get-in tile-layer [y x]))

(defn assoc-tile
  "Associates a tile to a tile layer at the given coordinates."
  [tile-layer x y tile]
  (assoc-in tile-layer [y x] tile))

(defn resize
  "Resizes a tile layer."
  [tile-layer new-width new-height]
  (let [old-width (width tile-layer)
        old-height (height tile-layer)]
    (condp = 0
      (* new-width new-height) []
      ;; Create zero-initialized 2D vector
      (* old-width old-height) (vec (repeat new-height (vec (repeat new-width 0))))
      (as-> tile-layer resized
        ;; Resize the height
        (cond
          (= new-height old-height) resized
          ;; Append zero-initialized 2D vector to existing tile-layer
          (> new-height old-height) (vec (concat resized (repeat (- new-height old-height) (vec (repeat new-width 0)))))
          ;; Cut height to proper size
          :else (subvec resized 0 new-height))
        ;; Resize the width
        (cond
          (= new-width old-width) resized
          ;; Append zero-initialized vector to each existing row
          (> new-width old-width) (mapv #(vec (concat % (repeat (- new-width old-width) 0))) resized)
          ;; Cut each row to proper size
          :else (mapv #(subvec % 0 new-width) resized))))))

(spec/def ::tile-layer (spec/coll-of
                        (spec/coll-of ::util/ubyte :max-count max-width :kind vector?)
                        :max-count max-height
                        :kind vector?))

(def tile-layer-codec
  "Codec for a PxPack tile layer."
  (bin/compile-codec
   (bin/header
    (bin/ordered-map
     :header (bin/constant (bin/c-string "UTF-8") header)
     :width :ushort-le
     :height :ushort-le)
    (fn [{:keys [width height]}]
      (if (zero? (* width height))
        []
        [(bin/constant :byte 0) (bin/repeated (bin/repeated :ubyte :length width) :length height)]))
    nil
    :keep-header? true)
   ;; Convert tile layer into file data
   (fn [layer]
     (let [w (width layer)
           h (height layer)]
       {:header {:header header :width w :height h}
        :body (if (zero? (* w h)) [] [0 layer])}))
   ;; Turn file data into 2D vector
   (comp (partial mapv vec) second :body)))
