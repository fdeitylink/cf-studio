(ns kero-edit.edit.menu-bar
  (:require [cljfx.api :as fx]
            [kero-edit.edit.i18n :refer [sub-translate]]))

(defn file-menu
  [{:keys [fx/context]}]
  {:fx/type :menu
   :text (fx/sub context sub-translate ::file)
   :items [{:fx/type :menu-item
            :text (fx/sub context sub-translate ::file-open)
            :accelerator [:ctrl :o]
            :on-action {:event/type ::open-new-mod}}
           {:fx/type :menu-item
            :text (fx/sub context sub-translate ::file-open-last)
            :accelerator [:ctrl :l]
            :on-action {:event/type ::open-last-mod}}
           {:fx/type :menu-item
            :text (fx/sub context sub-translate ::file-save)
            :accelerator [:ctrl :s]
            :on-action {:event/type ::save-file}}
           {:fx/type :menu-item
            :text (fx/sub context sub-translate ::file-save-all)
            :accelerator [:ctrl :shift :s]
            :on-action {:event/type ::save-all-files}}
           {:fx/type :menu-item
            :text (fx/sub context sub-translate ::file-close-tab)
            :accelerator [:ctrl :w]
            :on-action {:event/type ::close-tab}}
           {:fx/type :menu-item
            :text (fx/sub context sub-translate ::file-close-all-tabs)
            :accelerator [:ctrl :shift :w]
            :on-action {:event/type ::close-all-tabs}}]})

(defn edit-menu
  [{:keys [fx/context]}]
  {:fx/type :menu
   :text (fx/sub context sub-translate ::edit)
   :items [{:fx/type :menu-item
            :text (fx/sub context sub-translate ::edit-undo)
            :accelerator [:ctrl :z]
            :on-action {:event/type ::undo}}
           {:fx/type :menu-item
            :text (fx/sub context sub-translate ::edit-redo)
            :accelerator [:ctrl :y]
            :on-action {:event/type ::redo}}]})

(defn view-menu
  [{:keys [fx/context]}]
  {:fx/type :menu
   :text (fx/sub context sub-translate ::view)
   :items [{:fx/type :menu
            :text (fx/sub context sub-translate ::view-map-zoom)}
           {:fx/type :menu
            :text (fx/sub context sub-translate ::view-tileset-zoom)}
           {:fx/type :menu-item
            :text (fx/sub context sub-translate ::view-tileset-background-color)
            :on-action {:event/type ::choose-tileset-background-color}}]})

(defn actions-menu
  [{:keys [fx/context]}]
  {:fx/type :menu
   :text (fx/sub context sub-translate ::actions)
   :items [{:fx/type :menu-item
            :text (fx/sub context sub-translate ::actions-run-game)
            :accelerator [:f5]
            :on-action {:event/type ::run-mod}}
           {:fx/type :menu-item
            :text (fx/sub context sub-translate ::actions-edit-global-script)
            :on-action {:event/type ::edit-global-script}}
           {:fx/type :menu-item
            :text (fx/sub context sub-translate ::actions-waffle)
            :on-action {:event/type ::print-waffle}}]})

(defn help-menu
  [{:keys [fx/context]}]
  {:fx/type :menu
   :text (fx/sub context sub-translate ::help)
   :items [{:fx/type :menu-item
            :text (fx/sub context sub-translate ::help-about)
            :on-action {:event/type ::show-about}}
           #_{:fx/type :menu-item
              :text (fx/sub context sub-translate ::help-guide)
              :on-action {:event/type ::show-guide}}]})

(defn menu-bar
  [{:keys [fx/context]}]
  {:fx/type :menu-bar
   :menus [{:fx/type file-menu}
           {:fx/type edit-menu}
           {:fx/type view-menu}
           {:fx/type actions-menu}
           {:fx/type help-menu}]})
