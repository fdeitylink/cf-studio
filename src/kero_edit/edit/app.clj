(ns kero-edit.edit.app
  (:require [me.raynes.fs :as fs]
            [cljfx.api :as fx]
            [kero-edit.kero.gamedata :as gamedata]
            [kero-edit.edit.config :as config]
            [kero-edit.edit.i18n :refer [translate-sub]]
            [kero-edit.edit.events :as events]
            [kero-edit.edit.effects :as effects]
            [kero-edit.edit.menu-bar :refer [menu-bar]]
            [kero-edit.edit.settings-view :refer [settings-view]])
  (:gen-class))

(def *context
  (atom (fx/create-context (config/read-config))))

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
                                                  :text (slurp "LICENSE")
                                                  :v-box/vgrow :always}]}}})

(defn notepad-tab
  [{:keys [fx/context]}]
  {:fx/type :tab
   :text (fx/sub context translate-sub ::notepad-title)
   :id (fx/sub context translate-sub ::notepad-title)
   :closable false
   :content {:fx/type :text-area
             :text (fx/sub context :notepad-text)
             :on-text-changed {::events/type ::events/notepad-text-changed}}})

(defn tabs-view
  [{:keys [fx/context]}]
  {:fx/type :tab-pane
   :tabs [{:fx/type notepad-tab}]})

(defn field-list
  [{:keys [fx/context]}]
  {:fx/type :v-box
   :children [{:fx/type :label
               :padding {:top 10 :bottom 10 :left 10}
               :text (fx/sub context translate-sub ::field-list-label)
               :font {:family "" :weight :bold :size 20}}
              {:fx/type :list-view
               :cell-factory (fn [path] {:text (fs/base-name path true)})
               :items (sequence (::gamedata/fields (fx/sub context :gamedata)))
               :v-box/vgrow :always}]})

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
  [& args]
  (fx/create-app
   *context
   :event-handler events/event-handler
   :effects {::effects/choose-file effects/choose-file-effect
             ::effects/read-file effects/read-file-effect
             ::effects/write-file effects/write-file-effect
             ::effects/exception-dialog effects/exception-dialog-effect
             ::effects/shutdown effects/shutdown-effect}
   :desc-fn (fn [context]
              (if (fx/sub context :license-accepted)
                {:fx/type root-view}
                {:fx/type license-dialog}))))
