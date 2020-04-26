(ns kero-edit.edit.field-list
  (:require [me.raynes.fs :as fs]
            [cljfx.api :as fx]
            [cljfx.ext.list-view :as fx.ext.list-view]
            [kero-edit.kero.metadata :as metadata]
            [kero-edit.edit.i18n :refer [translate-sub]]
            [kero-edit.edit.events :as events]))

(defn context-menu
  [{:keys [fx/context]}]
  {:fx/type :context-menu
   :items [{:fx/type :menu-item
            :text (fx/sub context translate-sub ::open)
            :accelerator [:enter]
            :disable (not (fx/sub context :metadata))
            :on-action {::events/type ::events/open-fields}}
           #_{:fx/type :menu-item
              :text (fx/sub context translate-sub ::delete)
              :accelerator [:delete]
              :disable (not (fx/sub context :metadata))
              :on-action {::events/type ::events/delete-fields}}]})

(defn field-list
  [{:keys [fx/context]}]
  {:fx/type :v-box
   :children [{:fx/type :label
               :padding 10
               :text (fx/sub context translate-sub ::label)
               :font {:family "" :weight :bold :size 20}}
              {:fx/type fx.ext.list-view/with-selection-props
               :v-box/vgrow :always
               :props {:selection-mode :multiple
                       :on-selected-items-changed {::events/type ::events/field-selection-changed}}
               :desc {:fx/type :list-view
                      :cell-factory (fn [path] {:text (fs/base-name path true)})
                      :items (sequence (::metadata/fields (fx/sub context :metadata)))
                      :context-menu {:fx/type context-menu}
                      :on-mouse-clicked {::events/type ::events/field-list-click}
                      :on-key-pressed {::events/type ::events/field-list-keypress}}}]})
