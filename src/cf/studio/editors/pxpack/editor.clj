(ns cf.studio.editors.pxpack.editor
  (:require [cf.kero.field.metadata :as metadata]
            [cf.kero.field.pxpack :as pxpack]
            [cf.studio.editors.pxpack.tile-layers :refer [tile-layers-editor]]
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

(defn pxpack-editor
  [{:keys [fx/context path]}]
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
                                 (get-in [::pxpack/metadata ::metadata/name]))
                       :text-alignment :right
                       :style-class "app-text-small"}
               :bottom {:fx/type :tab-pane
                        :tab-closing-policy :unavailable
                        :tabs (doall
                               (for [[text editor] {::metadata pxpack-metadata-editor
                                                    ::tile-layers tile-layers-editor}]
                                 {:fx/type :tab
                                  :text (fx/sub context translate-sub text)
                                  :content {:fx/type editor
                                            :path path}}))}}]})
