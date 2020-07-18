(ns cf.studio.app
  (:gen-class)
  (:require [cf.studio.config :as config]
            [cf.studio.editors.editor-view :refer [editor-view]]
            [cf.studio.effects :as effects]
            [cf.studio.events.core :as events]
            [cf.studio.file-graph :refer [new-file-graph]]
            [cf.studio.file-list :refer [file-list]]
            [cf.studio.i18n :refer [translate-sub]]
            [cf.studio.menu-bar :refer [menu-bar]]
            [cf.studio.styles :refer [styles]]
            [cf.util :as util]
            [cljfx.api :as fx]
            [cljfx.css :as css]))

;; TODO
;; init context fn (useful for ::events/clear-mod)
;; maybe drop game data wrapper and put executable & resource dir at top level

(def *context
  (atom (fx/create-context {:files (new-file-graph)})))

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

(defn root-view
  [{:keys [fx/context]}]
  {:fx/type :stage
   :title (fx/sub context translate-sub ::app-title)
   :maximized true
   :showing (fx/sub context :license-accepted)
   :on-hidden {::events/type ::events/shutdown}
   :scene {:fx/type :scene
           :stylesheets [(::css/url styles)]
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
  (util/set-running-in-repl!)
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
