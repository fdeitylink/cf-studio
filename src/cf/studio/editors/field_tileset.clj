(ns cf.studio.editors.field-tileset
  (:require [cf.kero.field.metadata :as metadata]
            [cf.kero.field.pxpack :as pxpack]
            [cf.studio.file-graph :as file-graph]
            [cf.util :as util]
            [cljfx.api :as fx]
            [me.raynes.fs :as fs])
  (:import javafx.scene.canvas.Canvas
           javafx.scene.image.Image))

(defn layer->tileset-sub
  "Returns the tileset image for `layer` in the field at `path`."
  [context path layer]
  (let [tileset-name (-> context
                         (fx/sub-ctx file-graph/file-data-sub path)
                         (get-in [::pxpack/metadata ::metadata/layer-metadata layer ::metadata/tileset]))]
    (some->> path
             (fx/sub-ctx context file-graph/file-dependencies-sub)
             ;; TODO filter file ext/type as well?
             (util/find-first #(= tileset-name (fs/base-name % true)))
             (fx/sub-ctx context file-graph/file-data-sub))))

(defn field-tileset-view
  [{:keys [fx/context path]}]
  (let [layer (fx/sub-val context get-in [:editors path :layer])
        image ^Image (fx/sub-ctx context layer->tileset-sub path layer)
        scale (fx/sub-val context get-in [:editors path :tileset-scale])
        [width height] (if-not image
                         [0 0]
                         (map (partial * scale) [(.getWidth image) (.getHeight image)]))]
    {:fx/type :scroll-pane
     :content {:fx/type :canvas
               :width width
               :height height
               :draw (fn [^Canvas canvas]
                       (doto (.getGraphicsContext2D canvas)
                         (.setImageSmoothing false)
                         (.clearRect 0 0 width height)
                         (.drawImage image 0 0 width height)))}}))
