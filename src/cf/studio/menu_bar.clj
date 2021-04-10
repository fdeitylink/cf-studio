(ns cf.studio.menu-bar
  (:require [cf.kero.game-data :as game-data]
            [cf.studio.effects :as effects]
            [cf.studio.events :as events]
            [cf.studio.file-graph :as file-graph]
            [cf.studio.i18n :refer [translate-sub]]
            [cljfx.api :as fx]
            [me.raynes.fs :as fs]))

(defn- file-menu
  [{:keys [fx/context]}]
  {:fx/type :menu
   :text (fx/sub-val context translate-sub ::file)
   :items [{:fx/type :menu-item
            :text (fx/sub-val context translate-sub ::file-open)
            :accelerator [:ctrl :o]
            :on-action {::events/type ::open-new-mod}}
           {:fx/type :menu-item
            :text (fx/sub-val context translate-sub ::file-open-last)
            :accelerator [:ctrl :l]
            :disable (not (fs/exists? (fx/sub-val context :last-executable-path)))
            :on-action {::events/type ::open-last-mod}}
           {:fx/type :menu-item
            :text (fx/sub-val context translate-sub ::file-close-mod)
            :disable (not (fx/sub-val context :game-data))
            :on-action {::events/type ::close-mod}}
           {:fx/type :menu-item
            :text (fx/sub-val context translate-sub ::file-save)
            :accelerator [:ctrl :s]
            :disable (not (fx/sub-val context :game-data))
            :on-action {::events/type ::save-file}}
           {:fx/type :menu-item
            :text (fx/sub-val context translate-sub ::file-save-all)
            :accelerator [:ctrl :shift :s]
            :disable (not (fx/sub-val context :game-data))
            :on-action {::events/type ::save-all-files}}
           #_{:fx/type :menu-item
              :text (fx/sub-val context translate-sub ::file-close-tab)
              :accelerator [:ctrl :w]
              :disable (not (fx/sub-val context :game-data))
              :on-action {::events/type ::close-tab}}
           #_{:fx/type :menu-item
              :text (fx/sub-val context translate-sub ::file-close-all-tabs)
              :accelerator [:ctrl :shift :w]
              :disable (not (fx/sub-val context :game-data))
              :on-action {::events/type ::close-all-tabs}}]})

(defn- edit-menu
  [{:keys [fx/context]}]
  {:fx/type :menu
   :text (fx/sub-val context translate-sub ::edit)
   :items [{:fx/type :menu-item
            :text (fx/sub-val context translate-sub ::edit-undo)
            :accelerator [:ctrl :z]
            :disable (not (fx/sub-val context :game-data))
            :on-action {::events/type ::undo}}
           {:fx/type :menu-item
            :text (fx/sub-val context translate-sub ::edit-redo)
            :accelerator [:ctrl :y]
            :disable (not (fx/sub-val context :game-data))
            :on-action {::events/type ::redo}}]})

(defn- view-menu
  [{:keys [fx/context]}]
  {:fx/type :menu
   :text (fx/sub-val context translate-sub ::view)
   :items [#_{:fx/type :menu
              :text (fx/sub-val context translate-sub ::view-map-zoom)}
           #_{:fx/type :menu
              :text (fx/sub-val context translate-sub ::view-tileset-zoom)}
           {:fx/type :menu-item
            :text (fx/sub-val context translate-sub ::view-tileset-background-color)
            :on-action {::events/type ::choose-tileset-background-color}}]})

(defn- actions-menu
  [{:keys [fx/context]}]
  {:fx/type :menu
   :text (fx/sub-val context translate-sub ::actions)
   :items [{:fx/type :menu-item
            :text (fx/sub-val context translate-sub ::actions-run-game)
            :accelerator [:f5]
            :disable (not (fx/sub-val context :game-data))
            :on-action {::events/type ::run-mod}}
           #_{:fx/type :menu-item
              :text (fx/sub-val context translate-sub ::actions-edit-global-script)
              :disable (not (fx/sub-val context :game-data))
              :on-action {::events/type ::edit-global-script}}
           {:fx/type :menu-item
            :text (fx/sub-val context translate-sub ::actions-waffle)
            :on-action {::events/type ::print-waffle}}]})

(defn- help-menu
  [{:keys [fx/context]}]
  {:fx/type :menu
   :text (fx/sub-val context translate-sub ::help)
   :items [{:fx/type :menu-item
            :text (fx/sub-val context translate-sub ::help-about)
            :on-action {::events/type ::show-about}}
           #_{:fx/type :menu-item
              :text (fx/sub-val context translate-sub ::help-guide)
              :on-action {::events/type ::show-guide}}]})

(defn menu-bar
  [_]
  {:fx/type :menu-bar
   :use-system-menu-bar true
   :menus [{:fx/type file-menu}
           {:fx/type edit-menu}
           {:fx/type view-menu}
           {:fx/type actions-menu}
           {:fx/type help-menu}]})

;; FIXME Choose file effect occurs before close mod event. May need feature request from cljfx
(defmethod events/event-handler ::open-new-mod
  [{:keys [fx/context]}]
  {:dispatch {::events/type ::close-mod}
   ::effects/choose-file {:title (fx/sub-val context translate-sub ::open-new-mod-chooser-title)
                          :initial-directory (if-let [last-path (fx/sub-val context :last-executable-path)]
                                               (fs/parent last-path)
                                               (fs/home))
                          :extension-filters [{:description (fx/sub-val context translate-sub ::open-new-mod-filter-description)
                                               :extensions ["*.exe"]}]
                          :dialog-type :open
                          :on-complete {::events/type ::open-mod}}})

(defmethod events/event-handler ::open-last-mod
  [{:keys [fx/context]}]
  [[:dispatch {::events/type ::close-mod}]
   [:dispatch {::events/type ::open-mod :path (fx/sub-val context :last-executable-path)}]])

(defmethod events/event-handler ::open-mod
  [{:keys [fx/context path]}]
  {:context (fx/swap-context context assoc :last-executable-path (str path))
   ::effects/read-file {:file {:path path}
                        :reader-fn game-data/executable->game-data
                        :on-complete {::events/type ::load-mod}
                        :on-exception {::events/type ::events/exception}}})

(defmethod events/event-handler ::load-mod
  [{:keys [fx/context] {:keys [data]} :file}]
  {:context (fx/swap-context context assoc
                             :game-data (select-keys data [::game-data/executable ::game-data/resource-dir])
                             :files (apply
                                     file-graph/file-graph
                                     (mapcat
                                      (fn [[type paths]] (map (fn [p] {:type type :path p}) paths))
                                      (dissoc data ::game-data/executable ::game-data/resource-dir))))})

;; TODO Create prompt effect
;; Checks for unsaved work and only clears everything if there is none or user still wants to close
(defmethod events/event-handler ::close-mod
  [{:keys [fx/context]}]
  (concat
   (for [path (file-graph/paths (fx/sub-ctx context file-graph/filter-editing-files-sub))]
     [:dispatch {::events/type ::events/close-editor :path path}])
   {:dispatch {::events/type ::clear-mod}}))

(defmethod events/event-handler ::clear-mod
  [{:keys [fx/context]}]
  {:context (fx/swap-context context #(-> %
                                          (dissoc :game-data :selected-path :editor)
                                          (assoc :files (file-graph/file-graph))))})
