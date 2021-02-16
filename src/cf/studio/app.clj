(ns cf.studio.app
  (:gen-class)
  (:require [cf.studio.config :as config]
            [cf.studio.editors.editor-view :refer [editor-view]]
            [cf.studio.effects :as effects]
            [cf.studio.events.core :as events]
            [cf.studio.file-graph :refer [file-graph]]
            [cf.studio.file-list :refer [file-list]]
            [cf.studio.i18n :refer [translate-sub]]
            [cf.studio.menu-bar :refer [menu-bar]]
            [cf.studio.styles :refer [styles]]
            [cf.util :as util]
            [cljfx.api :as fx]
            [cljfx.css :as css]))

;; TODO Maybe drop game data wrapper and put executable & resource dir at top level

;; TODO Move this to another ns so its usable for :events/clear-mod
(defn- init-context
  [config]
  (atom (fx/create-context (assoc config :files (file-graph)))))

(defn root-view
  [{:keys [fx/context]}]
  {:fx/type :stage
   :title (fx/sub-val context translate-sub ::app-title)
   :maximized true
   :showing true
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
  (fx/create-app
   (init-context (config/read-config! config-path))
   :event-handler events/event-handler
   :effects {::effects/choose-file effects/choose-file
             ::effects/read-file effects/read-file
             ::effects/write-file effects/write-file
             ::effects/exception-dialog effects/exception-dialog
             ::effects/shutdown effects/shutdown}
   :desc-fn (fn [_] {:fx/type root-view})))
