(ns cf.studio.editors.pxpack.editor
  (:require [cf.kero.field.metadata :as metadata]
            [cf.kero.field.pxpack :as pxpack]
            [cf.studio.events.core :as events]
            [cf.studio.file-graph :as file-graph]
            [cf.studio.i18n :refer [translate-sub]]
            [cljfx.api :as fx]
            [me.raynes.fs :as fs]))

(defn- child-editor-text
  [type]
  {:fx/type :text
   :text (str type)
   :style-class "app-text-small"})

(defn- pxpack-metadata-editor
  [{:keys [path]}]
  (child-editor-text ::pxpack/metadata))

(defn- pxpack-tile-layers-editor
  [{:keys [path]}]
  (child-editor-text ::pxpack/tile-layers))

(defn- pxpack-units-editor
  [{:keys [path]}]
  (child-editor-text ::pxpack/units))

(defn- child-editor
  [{:keys [path subtype]}]
  (case subtype
    ::pxpack/metadata {:fx/type pxpack-metadata-editor :path path}
    ::pxpack/tile-layers {:fx/type pxpack-tile-layers-editor :path path}
    ::pxpack/units {:fx/type pxpack-units-editor :path path}))

(defn pxpack-editor
  [{:keys [fx/context path subtype]}]
  {:fx/type :v-box
   :alignment :top-center
   :children [{:fx/type :border-pane
               :left {:fx/type :text
                      :text (fs/base-name path true)
                      :text-alignment :left
                      :style-class "app-title"}
               :right {:fx/type :text
                       :text (-> context
                                 (fx/sub file-graph/file-data-sub path)
                                 (get-in [::pxpack/metadata ::metadata/description]))
                       :text-alignment :right
                       :style-class "app-text-small"}
               :bottom {:fx/type :h-box
                        :children (doall
                                   (for [[text subtype] [[::metadata ::pxpack/metadata]
                                                         [::tile-layers ::pxpack/tile-layers]
                                                         [::units ::pxpack/units]]]
                                     {:fx/type :button
                                      :h-box/margin {:top 5.0 :bottom 5.0}
                                      :text (fx/sub context translate-sub text)
                                      :on-action {::events/type ::events/switch-pxpack-editor :path path :subtype subtype}}))}}
              {:fx/type :separator}
              {:fx/type child-editor
               :path path
               :subtype subtype
               :v-box/vgrow :always}]})
