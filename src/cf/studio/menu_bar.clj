(ns cf.studio.menu-bar
  (:require [cf.studio.events.core :as events]
            [cf.studio.i18n :refer [translate-sub]]
            [cljfx.api :as fx]
            [me.raynes.fs :as fs]))

(defn- file-menu
  [{:keys [fx/context]}]
  {:fx/type :menu
   :text (fx/sub context translate-sub ::file)
   :items [{:fx/type :menu-item
            :text (fx/sub context translate-sub ::file-open)
            :accelerator [:ctrl :o]
            :on-action {::events/type ::events/open-new-mod}}
           {:fx/type :menu-item
            :text (fx/sub context translate-sub ::file-open-last)
            :accelerator [:ctrl :l]
            :disable (not (fs/exists? (fx/sub context :last-executable-path)))
            :on-action {::events/type ::events/open-last-mod}}
           {:fx/type :menu-item
            :text (fx/sub context translate-sub ::file-close-mod)
            :disable (not (fx/sub context :game-data))
            :on-action {::events/type ::events/close-mod}}
           {:fx/type :menu-item
            :text (fx/sub context translate-sub ::file-save)
            :accelerator [:ctrl :s]
            :disable (not (fx/sub context :game-data))
            :on-action {::events/type ::events/save-file}}
           {:fx/type :menu-item
            :text (fx/sub context translate-sub ::file-save-all)
            :accelerator [:ctrl :shift :s]
            :disable (not (fx/sub context :game-data))
            :on-action {::events/type ::events/save-all-files}}
           #_{:fx/type :menu-item
            :text (fx/sub context translate-sub ::file-close-tab)
            :accelerator [:ctrl :w]
            :disable (not (fx/sub context :game-data))
            :on-action {::events/type ::events/close-tab}}
           #_{:fx/type :menu-item
            :text (fx/sub context translate-sub ::file-close-all-tabs)
            :accelerator [:ctrl :shift :w]
            :disable (not (fx/sub context :game-data))
            :on-action {::events/type ::events/close-all-tabs}}]})

(defn- edit-menu
  [{:keys [fx/context]}]
  {:fx/type :menu
   :text (fx/sub context translate-sub ::edit)
   :items [{:fx/type :menu-item
            :text (fx/sub context translate-sub ::edit-undo)
            :accelerator [:ctrl :z]
            :disable (not (fx/sub context :game-data))
            :on-action {::events/type ::events/undo}}
           {:fx/type :menu-item
            :text (fx/sub context translate-sub ::edit-redo)
            :accelerator [:ctrl :y]
            :disable (not (fx/sub context :game-data))
            :on-action {::events/type ::events/redo}}]})

(defn- view-menu
  [{:keys [fx/context]}]
  {:fx/type :menu
   :text (fx/sub context translate-sub ::view)
   :items [#_{:fx/type :menu
            :text (fx/sub context translate-sub ::view-map-zoom)}
           #_{:fx/type :menu
            :text (fx/sub context translate-sub ::view-tileset-zoom)}
           {:fx/type :menu-item
            :text (fx/sub context translate-sub ::view-tileset-background-color)
            :on-action {::events/type ::events/choose-tileset-background-color}}]})

(defn- actions-menu
  [{:keys [fx/context]}]
  {:fx/type :menu
   :text (fx/sub context translate-sub ::actions)
   :items [{:fx/type :menu-item
            :text (fx/sub context translate-sub ::actions-run-game)
            :accelerator [:f5]
            :disable (not (fx/sub context :game-data))
            :on-action {::events/type ::events/run-mod}}
           #_{:fx/type :menu-item
            :text (fx/sub context translate-sub ::actions-edit-global-script)
            :disable (not (fx/sub context :game-data))
            :on-action {::events/type ::events/edit-global-script}}
           {:fx/type :menu-item
            :text (fx/sub context translate-sub ::actions-waffle)
            :on-action {::events/type ::events/print-waffle}}]})

(defn- help-menu
  [{:keys [fx/context]}]
  {:fx/type :menu
   :text (fx/sub context translate-sub ::help)
   :items [{:fx/type :menu-item
            :text (fx/sub context translate-sub ::help-about)
            :on-action {::events/type ::events/show-about}}
           #_{:fx/type :menu-item
              :text (fx/sub context translate-sub ::help-guide)
              :on-action {::events/type ::events/show-guide}}]})

(defn menu-bar
  [_]
  {:fx/type :menu-bar
   :use-system-menu-bar true
   :menus [{:fx/type file-menu}
           {:fx/type edit-menu}
           {:fx/type view-menu}
           {:fx/type actions-menu}
           {:fx/type help-menu}]})
