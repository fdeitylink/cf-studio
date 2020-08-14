(ns cf.studio.editors.pxpack.editor
  (:require [cf.kero.field.metadata :as metadata]
            [cf.kero.field.pxpack :as pxpack]
            [cf.studio.editors.pxpack.tile-layers :refer [tile-layers-editor]]
            [cf.studio.file-graph :as file-graph]
            [cf.studio.i18n :refer [translate-sub]]
            [cljfx.api :as fx]
            [me.raynes.fs :as fs]))

(defn- pxpack-metadata-editor
  [{:keys [path]}]
  {:fx/type :text
   :text (str ::pxpack/metadata)
   :style-class "app-text-small"})

(defn pxpack-editor
  [{:keys [fx/context path]}]
  {:fx/type :v-box
   :children [{:fx/type :h-box
               :children [{:fx/type :text
                           :text (fs/base-name path true)
                           :h-box/margin 10
                           :style-class "app-title"}
                          ;; Filler to push the text to the corners
                          {:fx/type :region
                           :h-box/hgrow :always}
                          {:fx/type :text
                           :h-box/margin 10
                           :text (-> context
                                     (fx/sub-ctx file-graph/file-data-sub path)
                                     (get-in [::pxpack/metadata ::metadata/name]))
                           :style-class "app-text-small"}]}
              {:fx/type :tab-pane
               :tab-closing-policy :unavailable
               :v-box/vgrow :always
               :tabs (doall
                      (for [[text editor] {::metadata pxpack-metadata-editor
                                           ::tile-layers tile-layers-editor}]
                        {:fx/type :tab
                         :text (fx/sub-ctx context translate-sub text)
                         :content {:fx/type editor
                                   :path path}}))}]})
