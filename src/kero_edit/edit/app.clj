(ns kero-edit.edit.app
  (:require [clojure.java.io :as io]
            [cljfx.api :as fx]
            [kero-edit.edit.config :as config]
            [kero-edit.edit.i18n :refer [translate-sub]]
            [kero-edit.edit.events :as events]
            [kero-edit.edit.menu-bar :refer [menu-bar]]
            [kero-edit.edit.settings-view :refer [settings-view]])
  (:import [javafx.scene.text Font FontWeight]))

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
                                                  :text (slurp (io/resource "LICENSE"))
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
  {:fx/type :list-view})

(defn root-view
  [{:keys [fx/context]}]
  {:fx/type :stage
   :title "Kero Edit"
   :maximized true
   :showing (fx/sub context :license-accepted)
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

(def app
  (fx/create-app
   *context
   :event-handler events/event-handler
   :desc-fn (fn [context]
              (if (fx/sub context :license-accepted)
                {:fx/type root-view}
                {:fx/type license-dialog}))))
