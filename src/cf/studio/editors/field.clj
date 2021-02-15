(ns cf.studio.editors.field
  (:require [cf.kero.field.metadata :as metadata]
            [cf.kero.field.pxpack :as pxpack]
            [cf.studio.editors.field-layers :refer [field-layers-view]]
            [cf.studio.editors.field-prefs :refer [field-prefs-view]]
            [cf.studio.editors.field-tileset :refer [field-tileset-view]]
            [cf.studio.file-graph :as file-graph]
            [cf.studio.i18n :refer [translate-sub]]
            [cljfx.api :as fx]
            [me.raynes.fs :as fs]))

(defn- field-metadata-editor
  [{:keys [path]}]
  {:fx/type :text
   :text (str ::pxpack/metadata)
   :style-class "app-text-small"})

(defn- field-layers-editor
  [{:keys [path]}]
  {:fx/type :split-pane
   :orientation :vertical
   :divider-positions [0.2]
   :items [{:fx/type :split-pane
            :items [{:fx/type field-prefs-view
                     :path path}
                    {:fx/type field-tileset-view
                     :path path}]}
           {:fx/type :scroll-pane
            :content {:fx/type field-layers-view
                      :path path}}]})

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
                         :text (fx/sub-val context translate-sub text)
                         :content {:fx/type editor
                                   :path path}}))}]})
