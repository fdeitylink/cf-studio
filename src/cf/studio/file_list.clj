(ns cf.studio.file-list
  (:require [cf.kero.field.pxpack :as pxpack]
            [cf.studio.events.core :as events]
            [cf.studio.file-graph :as file-graph]
            [cf.studio.i18n :refer [translate-sub]]
            [cf.util :as util]
            [cljfx.api :as fx]
            [cljfx.ext.tree-view :as fx.ext.tree-view]
            [clojure.set]
            [me.raynes.fs :as fs]))

(defn- context-menu
  [{:keys [fx/context]}]
  (let [path (fx/sub-val context :selected-path)]
    {:fx/type :context-menu
     :items [{:fx/type :menu-item
              :text (fx/sub-val context translate-sub ::open)
              ;; FIXME Accelerator doesn't work?
              :accelerator [:enter]
              :disable (not (fx/sub-val context :game-data))
              :on-action {::events/type ::events/open-selected-path}}
             #_{:fx/type :menu-item
                :text (fx/sub-val context translate-sub ::delete)
                :accelerator [:delete]
                :disable (not (fx/sub-val context :game-data))
                :on-action {::events/type ::events/delete-file}}
             {:fx/type :menu-item
              :text (fx/sub-val context translate-sub ::close)
              :accelerator [:ctrl :w]
              :disable (not (and path (fx/sub-ctx context file-graph/editing-file?-sub path)))
              :on-action {::events/type ::events/close-editor :path path}}]}))

(def ^:private resource-types
  "List of resource types that have editor implementations and thus appear in this tree view"
  [::pxpack/pxpack])

;; TODO Filter type, then open/closed?

(defn- editing-list-sub
  [context resource-type]
  (-> context
      (fx/sub-ctx file-graph/filter-editing-files-sub)
      (file-graph/filter-file-type resource-type)
      file-graph/paths
      sort))

;; TODO Remove eventually. Not very useful for the user.

(defn- open-list-sub
  [context resource-type]
  (-> context
      (fx/sub-ctx file-graph/filter-open-files-sub)
      (file-graph/filter-file-type resource-type)
      file-graph/paths
      (util/disj* (fx/sub-ctx context editing-list-sub resource-type))
      sort))

(defn- closed-list-sub
  [context resource-type]
  (-> context
      (fx/sub-ctx file-graph/filter-closed-files-sub)
      (file-graph/filter-file-type resource-type)
      file-graph/paths
      sort))

(defn- file-group
  [{:keys [fx/context resource-type]}]
  {:fx/type :tree-item
   :value (->> resource-type
               name
               (keyword "cf.studio.file-list")
               (fx/sub-val context translate-sub))
   :children (let [editing (fx/sub-ctx context editing-list-sub resource-type)
                   open (fx/sub-ctx context open-list-sub resource-type)
                   closed (fx/sub-ctx context closed-list-sub resource-type)]
               (doall
                (list*
                 {:fx/type :tree-item
                  :expanded true
                  :value (fx/sub-val context translate-sub ::editing-files)
                  :children (for [file editing]
                              {:fx/type :tree-item :value file})}
                 {:fx/type :tree-item
                  :value (fx/sub-val context translate-sub ::open-files)
                  :children (for [file open]
                              {:fx/type :tree-item :value file})}
                 (for [file closed]
                   {:fx/type :tree-item :value file}))))})

(defn file-list
  [{:keys [fx/context]}]
  {:fx/type :v-box
   :children [{:fx/type :label
               :text (fx/sub-val context translate-sub ::label)
               :style-class ["app-title" "label"]}
              {:fx/type fx.ext.tree-view/with-selection-props
               :v-box/vgrow :always
               :props {:selection-mode :single
                       :on-selected-item-changed {::events/type ::events/selected-path-changed}}
               :desc {:fx/type :tree-view
                      :show-root false
                      ;; TODO possible feature request
                      ;; It seems cell factories can't rely on cljfx context
                      ;; Otherwise, the values would be keywords (to be translated with translate-sub)
                      ;; Dev seems to allude to this here: https://github.com/cljfx/cljfx#factory-props
                      :cell-factory {:fx/cell-type :tree-cell
                                     :describe (fn [value]
                                                 {:style-class ["cell" "indexed-cell" "tree-cell" "app-file-list-cell"]
                                                  :text (if (string? value) value (fs/base-name value true))})}
                      :context-menu {:fx/type context-menu}
                      :on-mouse-clicked {::events/type ::events/file-list-click}
                      :on-key-pressed {::events/type ::events/file-list-keypress}
                      :root {:fx/type :tree-item
                             :children (doall
                                        (for [group resource-types]
                                          {:fx/type file-group
                                           :resource-type group}))}}}]})

