(ns cf.studio.app
  (:gen-class)
  (:require [cf.studio.config :as config]
            [cf.studio.editors.editor-view :refer [editor-view]]
            [cf.studio.effects :as effects]
            [cf.studio.events :as events]
            [cf.studio.file-list :refer [file-list]]
            [cf.studio.i18n :refer [translate-sub]]
            [cf.studio.menu-bar :refer [menu-bar]]
            [cljfx.api :as fx]))

(def *context
  (atom (fx/create-context {})))

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

#_(defn notepad-tab
  [{:keys [fx/context]}]
  {:fx/type :tab
   :id "notepad"
   :text (fx/sub context translate-sub ::notepad-title)
   :closable false
   :content {:fx/type :text-area
             :text (fx/sub context :notepad-text)
             :on-text-changed {::events/type ::events/notepad-text-changed}}})

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
                              ;; FIXME This is overridden when the stage maximizes
                              :divider-positions [0.1]
                              :v-box/vgrow :always
                              :items [{:fx/type file-list}
                                      {:fx/type editor-view}]}]}}})

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
