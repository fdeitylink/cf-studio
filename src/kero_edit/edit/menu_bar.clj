(ns kero-edit.edit.menu-bar
  (:require [me.raynes.fs :as fs]
            [cljfx.api :as fx]
            [kero-edit.edit.i18n :refer [translate-sub]]
            [kero-edit.edit.events :as events]))

(defn file-menu
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
            :on-action {::events/type ::events/open-last-mod}
            :disable (not (fx/sub context :last-executable-path))}
           {:fx/type :menu-item
            :text (fx/sub context translate-sub ::file-save)
            :accelerator [:ctrl :s]
            :on-action {::events/type ::events/save-file}
            :disable (not (fx/sub context :gamedata))}
           {:fx/type :menu-item
            :text (fx/sub context translate-sub ::file-save-all)
            :accelerator [:ctrl :shift :s]
            :on-action {::events/type ::events/save-all-files}
            :disable (not (fx/sub context :gamedata))}
           {:fx/type :menu-item
            :text (fx/sub context translate-sub ::file-close-tab)
            :accelerator [:ctrl :w]
            :on-action {::events/type ::events/close-tab}
            :disable (not (fx/sub context :gamedata))}
           {:fx/type :menu-item
            :text (fx/sub context translate-sub ::file-close-all-tabs)
            :accelerator [:ctrl :shift :w]
            :on-action {::events/type ::events/close-all-tabs}
            :disable (not (fx/sub context :gamedata))}]})

(defn edit-menu
  [{:keys [fx/context]}]
  {:fx/type :menu
   :text (fx/sub context translate-sub ::edit)
   :items [{:fx/type :menu-item
            :text (fx/sub context translate-sub ::edit-undo)
            :accelerator [:ctrl :z]
            :on-action {::events/type ::events/undo}
            :disable (not (fx/sub context :gamedata))}
           {:fx/type :menu-item
            :text (fx/sub context translate-sub ::edit-redo)
            :accelerator [:ctrl :y]
            :on-action {::events/type ::events/redo}
            :disable (not (fx/sub context :gamedata))}]})

(defn view-menu
  [{:keys [fx/context]}]
  {:fx/type :menu
   :text (fx/sub context translate-sub ::view)
   :items [{:fx/type :menu
            :text (fx/sub context translate-sub ::view-map-zoom)}
           {:fx/type :menu
            :text (fx/sub context translate-sub ::view-tileset-zoom)}
           {:fx/type :menu-item
            :text (fx/sub context translate-sub ::view-tileset-background-color)
            :on-action {::events/type ::events/choose-tileset-background-color}}]})

(defn actions-menu
  [{:keys [fx/context]}]
  {:fx/type :menu
   :text (fx/sub context translate-sub ::actions)
   :items [{:fx/type :menu-item
            :text (fx/sub context translate-sub ::actions-run-game)
            :accelerator [:f5]
            :on-action {::events/type ::events/run-mod}
            :disable (not (fx/sub context :gamedata))}
           {:fx/type :menu-item
            :text (fx/sub context translate-sub ::actions-edit-global-script)
            :on-action {::events/type ::events/edit-global-script}
            :disable (not (fx/sub context :gamedata))}
           {:fx/type :menu-item
            :text (fx/sub context translate-sub ::actions-waffle)
            :on-action {::events/type ::events/print-waffle}}]})

(defn help-menu
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
  [{:keys [fx/context]}]
  {:fx/type :menu-bar
   :menus [{:fx/type file-menu}
           {:fx/type edit-menu}
           {:fx/type view-menu}
           {:fx/type actions-menu}
           {:fx/type help-menu}]})
