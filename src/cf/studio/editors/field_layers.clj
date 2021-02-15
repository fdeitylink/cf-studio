(ns cf.studio.editors.field-layers
  (:require [cf.kero.field.metadata :as metadata]
            [cf.kero.field.pxpack :as pxpack]
            [cf.kero.field.tile-layer :as tile-layer]
            [cf.kero.tile.tileset :as tileset]
            [cf.studio.file-graph :as file-graph]
            [cf.studio.editors.field-tileset :refer [layer->tileset-sub]]
            [cljfx.api :as fx])
  (:import javafx.scene.canvas.Canvas
           [javafx.scene.image Image PixelFormat WritableImage WritablePixelFormat]
           javafx.scene.paint.Color))

(defn- tiles-buffer
  "Creates an int array for holding a tile layer region of the given size."
  [width height]
  (int-array (* width tileset/tile-width height tileset/tile-height)))

(def ^:private ^WritablePixelFormat px-format
  "[[WritablePixelFormat]] for use with rendering tile layers."
  (PixelFormat/getIntArgbPreInstance))

(defn- read-tile!
  "Reads the specified `tile` from `tileset` into `tiles-buf` at (`x`, `y`).
  `width` is the width of tile layer region being formed."
  [^ints tiles-buf ^Image tileset tile x y width]
  (let [[tx ty] (tileset/tile->xy tile)
        w (int (* width tileset/tile-width))]
    (.getPixels (.getPixelReader tileset)
                (int (* tx tileset/tile-width))
                (int (* ty tileset/tile-height))
                (int tileset/tile-width)
                (int tileset/tile-height)
                px-format
                tiles-buf
                (int (tileset/xy->tile
                      (* x tileset/tile-width)
                      (* y tileset/tile-height)
                      w))
                w)))

(defn- tile-layer-image
  "Creates an [[Image]] with the pixels from `tile-buf`.
  `width` and `height` are the dimensions of the tile layer region being formed."
  [^ints tiles-buf width height]
  (let [w (int (* width tileset/tile-width))
        h (int (* height tileset/tile-height))]
    (doto (WritableImage. w h)
      (-> .getPixelWriter
          (.setPixels
           0 0
           w h
           px-format
           tiles-buf
           0
           w)))))

(defn draw-tile-layer-region!
  "Draws the tiles, from `tileset`, in `tiles` in the specified region, scaled by `scale`, onto `canvas`."
  [^Canvas canvas ^Image tileset tiles x y width height scale]
  (future
    (when (and (some? tileset) (pos? (* width height)))
      (let [tiles-buf (tiles-buffer width height)
            img-x (* x tileset/tile-width scale)
            img-y (* y tileset/tile-height scale)
            img-w (* width tileset/tile-width scale)
            img-h (* height tileset/tile-height scale)]
        (doseq [x' (range x (+ x width))
                y' (range y (+ y height))]
          (let [tile (tile-layer/get-tile tiles x' y')]
            (read-tile! tiles-buf tileset tile (- x' x) (- y' y) width)))
        (doto (.getGraphicsContext2D canvas)
          (.clearRect img-x img-y img-w img-h)
          (.drawImage (tile-layer-image tiles-buf width height) img-x img-y img-w img-h))))))

(defn draw-tile!
  "Draws the tile in `tiles` at (`x`, `y`), scaled by `scale`, onto `canvas`."
  [^Canvas canvas ^Image tileset tiles x y scale]
  (draw-tile-layer-region!
   canvas
   tileset
   tiles
   x y
   1 1
   scale))

(defn draw-tile-layer!
  "Draws the tiles in `tiles`, scaled by `scale`, onto `canvas`."
  [^Canvas canvas ^Image tileset tiles scale]
  (draw-tile-layer-region!
   canvas
   tileset
   tiles
   0 0
   (tile-layer/width tiles)
   (tile-layer/height tiles)
   scale))

(defn- tile-layer-view
  [{:keys [fx/context path layer]}]
  (let [tiles (-> context
                  (fx/sub-ctx file-graph/file-data-sub path)
                  (get-in [::pxpack/tile-layers layer]))
        tileset (fx/sub-ctx context layer->tileset-sub path layer)
        visible (boolean (fx/sub-val context get-in [:editors path :visible-layers layer]))
        scale (fx/sub-val context get-in [:editors path :layer-scale])]
    {:fx/type :canvas
     :width (* scale tileset/tile-width (tile-layer/width tiles))
     :height (* scale tileset/tile-height (tile-layer/height tiles))
     :visible visible
     :draw (fn [^Canvas canvas]
             ;; TODO
             ;; This impl has lots of needless redrawing
             ;; i.e. when invisible -> visible,
             ;;      when one tile is changed in the whole layer
             ;; look into creating a mutable impl with more fine-grained control
             (when visible
               (doto canvas
                 (-> .getGraphicsContext2D
                     (.setImageSmoothing false))
                 (draw-tile-layer! tileset tiles scale))))}))

(defn field-layers-view
  [{:keys [fx/context path]}]
  (let [{::metadata/keys [red green blue]} (-> context
                                               (fx/sub-ctx file-graph/file-data-sub path)
                                               (get-in [::pxpack/metadata ::metadata/bg-color]))]
    {:fx/type :stack-pane
     ;; TODO CSS
     :alignment :top-left
     :background {:fills [{:fill (Color/rgb red green blue)
                           :radii :empty
                           :insets :empty}]}
     :children (mapv
                (fn [layer]
                  {:fx/type tile-layer-view
                   :path path
                   :layer layer})
                (reverse tile-layer/layers))}))
