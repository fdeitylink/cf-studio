(ns cf.studio.file-list
  (:require [cf.kero.field.pxpack :as pxpack]
            [cf.studio.events.core :as events]
            [cf.studio.file-graph :as file-graph]
            [cf.studio.i18n :refer [translate-sub]]
            [cljfx.api :as fx]
            [cljfx.ext.tree-view :as fx.ext.tree-view]
            [clojure.set]
            [me.raynes.fs :as fs]))

(defn- context-menu
  [{:keys [fx/context]}]
  (let [path (fx/sub context :selected-path)]
    {:fx/type :context-menu
     :items [{:fx/type :menu-item
              :text (fx/sub context translate-sub ::open)
              ;; FIXME Accelerator doesn't work?
              :accelerator [:enter]
              :disable (not (fx/sub context :game-data))
              :on-action {::events/type ::events/open-selected-file}}
             #_{:fx/type :menu-item
                :text (fx/sub context translate-sub ::delete)
                :accelerator [:delete]
                :disable (not (fx/sub context :game-data))
                :on-action {::events/type ::events/delete-file}}
             {:fx/type :menu-item
              :text (fx/sub context translate-sub ::close)
              :accelerator [:ctrl :w]
              :disable (not (and path (fx/sub context file-graph/editing-file?-sub path)))
              :on-action {::events/type ::events/close-editor :path path}}]}))

(def ^:private resource-types
  "List of resource types that have editor implementations and thus appear in this tree view"
  [::pxpack/pxpack])

(defn- file-group
  [{:keys [fx/context resource-type]}]
  {:fx/type :tree-item
   :value (->> resource-type
               name
               (keyword "cf.studio.file-list")
               (fx/sub context translate-sub))
   ;; TODO sort alphabetically
   :children (let [editing (-> context
                               (fx/sub file-graph/filter-editing-files-sub)
                               (file-graph/filter-file-type resource-type)
                               file-graph/paths)
                   open (-> context
                            (fx/sub file-graph/filter-open-files-sub)
                            (file-graph/filter-file-type resource-type)
                            file-graph/paths
                            (clojure.set/difference editing))
                   closed (-> context
                              (fx/sub file-graph/filter-closed-files-sub)
                              (file-graph/filter-file-type resource-type)
                              file-graph/paths)]
               (doall
                (list*
                 {:fx/type :tree-item
                  :expanded true
                  :value (fx/sub context translate-sub ::editing-files)
                  :children (for [file editing]
                              {:fx/type :tree-item :value file})}
                 {:fx/type :tree-item
                  :value (fx/sub context translate-sub ::open-files)
                  :children (for [file open]
                              {:fx/type :tree-item :value file})}
                 (for [file closed]
                   {:fx/type :tree-item :value file}))))})

(defn file-list
  [{:keys [fx/context]}]
  {:fx/type :v-box
   :children [{:fx/type :label
               :text (fx/sub context translate-sub ::label)
               :style-class ["app-title" "label"]}
              {:fx/type fx.ext.tree-view/with-selection-props
               :v-box/vgrow :always
               :props {:selection-mode :single
                       :on-selected-item-changed {::events/type ::events/selected-file-changed}}
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
