(ns kero-edit.edit.app
  (:require [clojure.java.io :as io]
            [cljfx.api :as fx]
            [kero-edit.edit.config :as config]
            [kero-edit.edit.i18n :refer [sub-translate]]
            [kero-edit.edit.menu-bar :refer [menu-bar]]
            [kero-edit.edit.settings-grid :refer [settings-grid]])
  (:import [javafx.scene.control Dialog DialogEvent ButtonType ButtonBar$ButtonData]
           [javafx.scene.text Font FontWeight]))

(def *context
  (atom (fx/create-context (config/read-config))))

(defmulti event-handler :event/type)

(defmethod event-handler ::notepad-text-changed [{:keys [fx/event fx/context]}]
  {:context (fx/swap-context context assoc ::notepad-text event)})

(defmethod event-handler ::license-dialog-consumed [{:keys [fx/event fx/context]}]
  {:context (fx/swap-context context assoc ::license-accepted
                             (= ButtonBar$ButtonData/YES (.getButtonData ^ButtonType (.getResult ^Dialog (.getSource event)))))})

(defn license-dialog
  [{:keys [fx/context]}]
  {:fx/type :dialog
   :title (fx/sub context sub-translate ::license-dialog-title)
   :showing (not (fx/sub context ::license-accepted))
   :on-hidden {:event/type ::license-dialog-consumed}
   :dialog-pane {:fx/type :dialog-pane
                 :button-types [:yes :no]
                 :expanded true
                 :expandable-content {:fx/type :v-box
                                      :spacing 20
                                      :alignment :center
                                      :children [{:fx/type :text
                                                  :text (fx/sub context sub-translate ::license-dialog-header)
                                                  :font (Font/font "" FontWeight/BOLD 20.0)}
                                                 {:fx/type :text-area
                                                  :text (slurp (io/resource "LICENSE"))
                                                  :v-box/vgrow :always}]}}})

(defn notepad-tab
  [{:keys [fx/context]}]
  {:fx/type :tab
   :text (fx/sub context sub-translate ::notepad-title)
   :id (fx/sub context sub-translate ::notepad-title)
   :closable false
   :content {:fx/type :text-area
             :text (fx/sub context ::notepad-text)
             :on-text-changed {:event/type ::notepad-text-changed}}})

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
   :showing (fx/sub context ::license-accepted)
   :scene {:fx/type :scene
           :root {:fx/type :v-box
                  :children [{:fx/type menu-bar}
                             {:fx/type :split-pane
                              :divider-positions [0.1]
                              :v-box/vgrow :always
                              :items [{:fx/type field-list}
                                      {:fx/type :v-box
                                       :children [{:fx/type settings-grid}
                                                  {:fx/type tabs-view
                                                   :v-box/vgrow :always}]}]}]}}})

(def app
  (fx/create-app
   *context
   :event-handler event-handler
   :desc-fn (fn [context]
              (if (fx/sub context ::license-accepted)
                {:fx/type root-view}
                {:fx/type license-dialog}))))
