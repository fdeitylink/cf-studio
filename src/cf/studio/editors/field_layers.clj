(ns cf.studio.editors.field-layers
  (:require [cf.kero.field.metadata :as metadata]
            [cf.kero.field.pxpack :as pxpack]
            [cf.kero.field.tile-layer :as tile-layer]
            [cf.kero.tile.tileset :as tileset]
            [cf.studio.events.core :as events]
            [cf.studio.i18n :refer [translate-sub]]
            [cf.studio.file-graph :as file-graph]
            [cf.util :as util]
            [cljfx.api :as fx]
            [me.raynes.fs :as fs])
  (:import javafx.scene.canvas.Canvas
           [javafx.scene.image Image PixelFormat WritableImage WritablePixelFormat]
           javafx.scene.paint.Color))

(defn- tiles-buffer
  "Creates an int array for holding a tile layer region of the given size."
  [width height]
  (int-array (* width tileset/tile-width height tileset/tile-height)))

(def ^:private ^WritablePixelFormat px-format
  "`WritablePixelFormat` for use with rendering tile layers."
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
  "Creates an `Image` with the pixels from `tile-buf`.
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
  "Draws the tiles in `tiles` in the specified region, scaled by `scale`, onto `canvas`."
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

(defn- layer->tileset-sub
  "Returns the tileset image for a field's layer."
  [context path layer]
  (let [tileset-name (-> context
                         (fx/sub-ctx file-graph/file-data-sub path)
                         (get-in [::pxpack/metadata ::metadata/layer-metadata layer ::metadata/tileset]))]
    (some->> path
             (fx/sub-ctx context file-graph/file-dependencies-sub)
             (util/find-first #(= tileset-name (fs/base-name % true)))
             (fx/sub-ctx context file-graph/file-data-sub))))

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

(defn- tileset-view
  [{:keys [fx/context path]}]
  (let [layer (fx/sub-val context get-in [:editors path :layer])
        image ^Image (fx/sub-ctx context layer->tileset-sub path layer)
        scale (fx/sub-val context get-in [:editors path :tileset-scale])
        [width height] (if-not image
                         [0 0]
                         (map
                          (partial * scale)
                          [(.getWidth image) (.getHeight image)]))]
    {:fx/type :scroll-pane
     :content {:fx/type :canvas
               :width width
               :height height
               :draw (fn [^Canvas canvas]
                       (doto (.getGraphicsContext2D canvas)
                         (.setImageSmoothing false)
                         (.clearRect 0 0 width height)
                         (.drawImage image 0 0 width height)))}}))

(defn- scale-slider
  [{:keys [fx/context text path value-key on-value-changed-event]}]
  {:fx/type :v-box
   :children [{:fx/type :label
               :text (fx/sub-val context translate-sub text)
               :style-class "app-title"}
              {:fx/type :slider
               :min 0.5
               :max 4
               :block-increment 0.5
               :major-tick-unit 1
               :minor-tick-count 1
               :show-tick-labels true
               :show-tick-marks true
               :snap-to-ticks true
               :value (fx/sub-val context get-in [:editors path value-key])
               :on-value-changed {::events/type on-value-changed-event
                                  :path path}}]})

(defn- editor-prefs-view
  [{:keys [fx/context path]}]
  {:fx/type :split-pane
   :items [{:fx/type :grid-pane
            :style-class "app-field-layer-editor-prefs"
            :children (flatten
                       [{:fx/type :label
                         :grid-pane/row 0
                         :grid-pane/column-span 2
                         :text (fx/sub-val context translate-sub ::layers)
                         :style-class "app-title"}
                        (vec
                         (for [[layer row] (map vector tile-layer/layers (range 1 4))]
                           [{:fx/type :radio-button
                             :grid-pane/row row
                             :selected (= layer (fx/sub-val context get-in [:editors path :layer]))
                             :on-selected-changed {::events/type ::events/pxpack-selected-tile-layer-changed
                                                   :path path
                                                   :layer layer}}
                            {:fx/type :check-box
                             :grid-pane/row row
                             :grid-pane/column 1
                             :selected (boolean (fx/sub-val context get-in [:editors path :visible-layers layer]))
                             :on-selected-changed {::events/type ::events/pxpack-visible-tile-layers-changed
                                                   :path path
                                                   :layer layer}
                             :style-class ["check-box" "app-text-small"]
                             :text (fx/sub-val context translate-sub (->> layer
                                                                          name
                                                                          (keyword "cf.studio.editors.field-layers")
                                                                          (fx/sub-val context translate-sub)))}]))
                        {:fx/type scale-slider
                         :grid-pane/row 4
                         :grid-pane/column-span 2
                         :text ::layer-scale
                         :path path
                         :value-key :layer-scale
                         :on-value-changed-event ::events/pxpack-tile-layer-scale-changed}
                        {:fx/type scale-slider
                         :grid-pane/row 5
                         :grid-pane/column-span 2
                         :text ::tileset-scale
                         :path path
                         :value-key :tileset-scale
                         :on-value-changed-event ::events/pxpack-tileset-scale-changed}])}
           {:fx/type tileset-view
            :path path}]})

(defn field-layers-editor
  [{:keys [fx/context path]}]
  (let [{::metadata/keys [red green blue]} (-> context
                                               (fx/sub-ctx file-graph/file-data-sub path)
                                               (get-in [::pxpack/metadata ::metadata/bg-color]))]
    {:fx/type :split-pane
     :orientation :vertical
     :divider-positions [0.2]
     :items [{:fx/type editor-prefs-view
              :path path}
             {:fx/type :scroll-pane
              :content {:fx/type :stack-pane
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
                                   (reverse tile-layer/layers))}}]}))
