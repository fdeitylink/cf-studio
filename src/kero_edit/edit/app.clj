(ns kero-edit.edit.app
  (:require [flatland.ordered.map :refer [ordered-map]]
            [me.raynes.fs :as fs]
            [cljfx.api :as fx]
            [kero-edit.edit.config :as config]
            [kero-edit.edit.i18n :refer [translate-sub]]
            [kero-edit.edit.events :as events]
            [kero-edit.edit.effects :as effects]
            [kero-edit.edit.menu-bar :refer [menu-bar]]
            [kero-edit.edit.field-list :refer [field-list]]
            [kero-edit.edit.settings-view :refer [settings-view]]
            [kero-edit.edit.field-edit-tab :refer [field-edit-tab]])
  (:gen-class))

(def *context
  (atom (fx/create-context {:selected-fields []
                            :loaded-fields (ordered-map)})))

(defn license-dialog
  [{:keys [fx/context]}]
  {:fx/type :dialog
   :title (fx/sub context translate-sub ::license-dialog-title)
   :showing (not (fx/sub context :license-accepted))
   :on-hidden {::events/type ::events/license-dialog-consumed}
   :dialog-pane {:fx/type :dialog-pane
                 :button-types [:yes :no]
                 :expanded true
                 :expandable-content {:fx/type :v-box
                                      :spacing 20
                                      :alignment :center
                                      :children [{:fx/type :text
                                                  :text (fx/sub context translate-sub ::license-dialog-header)
                                                  :font {:family "" :weight :bold :size 20}}
                                                 {:fx/type :text-area
                                                  :editable false
                                                  :text (slurp "LICENSE")
                                                  :v-box/vgrow :always}]}}})

(defn notepad-tab
  [{:keys [fx/context]}]
  {:fx/type :tab
   :id "notepad"
   :text (fx/sub context translate-sub ::notepad-title)
   :closable false
   :content {:fx/type :text-area
             :text (fx/sub context :notepad-text)
             :on-text-changed {::events/type ::events/notepad-text-changed}}})

(defn tabs-view
  [{:keys [fx/context]}]
  {:fx/type :tab-pane
 ;; TODO Fix issue: Notepad tab not always first tab - alternates with each field opened
   :tabs (cons {:fx/type notepad-tab}
               (for [field (vals (fx/sub context :loaded-fields))]
                 {:fx/type field-edit-tab
                  :field field}))})

(defn root-view
  [{:keys [fx/context]}]
  {:fx/type :stage
   :title (fx/sub context translate-sub ::app-title)
   :maximized true
   :showing (fx/sub context :license-accepted)
   :on-hidden {::events/type ::events/shutdown}
   :scene {:fx/type :scene
           :root {:fx/type :v-box
                  :children [{:fx/type menu-bar}
                             {:fx/type :split-pane
                              :divider-positions [0.1]
                              :v-box/vgrow :always
                              :items [{:fx/type field-list}
                                      {:fx/type :v-box
                                       :children [{:fx/type settings-view}
                                                  {:fx/type tabs-view
                                                   :v-box/vgrow :always}]}]}]}}})

(defn -main
  [& [config-path]]
  (let [{:keys [config config-path]} (config/read-config config-path)]
    (swap! *context fx/swap-context merge config {:config-path config-path}))
  (fx/create-app
   *context
   :event-handler events/event-handler
   :effects {::effects/choose-file effects/choose-file
             ::effects/read-file effects/read-file
             ::effects/write-file effects/write-file
             ::effects/exception-dialog effects/exception-dialog
             ::effects/shutdown effects/shutdown}
   :desc-fn (fn [context]
              (if (fx/sub context :license-accepted)
                {:fx/type root-view}
                {:fx/type license-dialog}))))
