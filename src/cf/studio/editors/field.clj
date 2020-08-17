(ns cf.studio.editors.field
  (:require [cf.kero.field.metadata :as metadata]
            [cf.kero.field.pxpack :as pxpack]
            [cf.studio.editors.field-layers :refer [field-layers-editor]]
            [cf.studio.file-graph :as file-graph]
            [cf.studio.i18n :refer [translate-sub]]
            [cljfx.api :as fx]
            [me.raynes.fs :as fs]))

(defn- field-metadata-editor
  [{:keys [path]}]
  {:fx/type :text
   :text (str ::pxpack/metadata)
   :style-class "app-text-small"})

(defn field-editor
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
               :v-box/vgrow :always
               :tab-closing-policy :unavailable
               :tabs (doall
                      (for [[text editor] {::metadata field-metadata-editor
                                           ::tile-layers field-layers-editor}]
                        {:fx/type :tab
                         :text (fx/sub-ctx context translate-sub text)
                         :content {:fx/type editor
                                   :path path}}))}]})
