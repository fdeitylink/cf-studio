(ns cf.studio.editors.pxpack-editor
  (:require [cf.kero.field.head :as head]
            [cf.kero.field.pxpack :as pxpack]
            [cf.studio.events :as events]
            [cf.studio.editors.events]
            [cf.studio.i18n :refer [translate-sub]]
            [cljfx.api :as fx]
            [me.raynes.fs :as fs]))

(defn- child-editor-text
  [{:keys [editor]}]
  {:fx/type :text
   :text (-> editor :type str)
   :font {:family "" :size 15}})

(defn- pxpack-head-editor
  [m]
  (child-editor-text m))

(defn- pxpack-tile-layers-editor
  [m]
  (child-editor-text m))

(defn- pxpack-units-editor
  [m]
  (child-editor-text m))

(defn- child-editor
  [editor]
  (case (:type editor)
    ::pxpack/head {:fx/type pxpack-head-editor :editor editor}
    ::pxpack/tile-layers {:fx/type pxpack-tile-layers-editor :editor editor}
    ::pxpack/units {:fx/type pxpack-units-editor :editor editor}))

(defn pxpack-editor
  [{:keys [fx/context editor]}]
  {:fx/type :v-box
   :alignment :top-center
   :children [{:fx/type :border-pane
               :left {:fx/type :text
                      :text (fs/base-name (:path editor) true)
                      :text-alignment :left
                      :font {:family "" :weight :bold :size 20}}
               :right {:fx/type :text
                       ;; A bit hacky, but editor refers to the sub-editor
                       ;; so this is the only way to get the description
                       ;; regardless of if the user is editing the head,
                       ;; tile layers, or units
                       :text (get-in
                              (fx/sub context :open-files)
                              [::pxpack/pxpack (:path editor) :data ::pxpack/head ::head/description])
                       :text-alignment :right
                       :font {:family "" :size 13}}
               :bottom {:fx/type :h-box
                        :spacing 5.0
                        :children (mapv
                                   #(assoc % :h-box/margin {:top 5.0 :bottom 5.0})
                                   (for [[text type] [[::head ::pxpack/head]
                                                      [::tile-layers ::pxpack/tile-layers]
                                                      [::units ::pxpack/units]]]
                                     {:fx/type :button
                                      :text (fx/sub context translate-sub text)
                                      :on-action {::events/type ::events/switch-pxpack-editor :path (:path editor) :type type}}))}}
              {:fx/type :separator}
              (child-editor editor)]})
