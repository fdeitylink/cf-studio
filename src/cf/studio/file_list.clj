(ns cf.studio.file-list
  (:require [cf.kero.field.pxpack :as pxpack]
            [cf.studio.events :as events]
            [cf.studio.i18n :refer [translate-sub]]
            [cljfx.api :as fx]
            [cljfx.ext.tree-view :as fx.ext.tree-view]
            [me.raynes.fs :as fs]))

(defn- context-menu
  [{:keys [fx/context]}]
  (let [file (fx/sub context :selected-file)]
    {:fx/type :context-menu
     :items [{:fx/type :menu-item
              :text (fx/sub context translate-sub ::open)
              ;; TODO Accelerator doesn't work?
              :accelerator [:enter]
              :disable (not (fx/sub context :metadata))}
              ;;:on-action {::events/type ::events/open-file :file file :create-editor? true}}
             #_{:fx/type :menu-item
                :text (fx/sub context translate-sub ::delete)
                :accelerator [:delete]
                :disable (not (fx/sub context :metadata))
                :on-action {::events/type ::events/delete-file}}
             {:fx/type :menu-item
              :text (fx/sub context translate-sub ::close)
              :accelerator [:ctrl :w]
              :disable (not (get (fx/sub context :editors) (:path file)))
              :on-action {::events/type ::events/close-editor :file file}}]}))

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
   :children (let [open (resource-type (fx/sub context :open-files))
                   not-open (remove (set (keys open)) (resource-type (fx/sub context :metadata)))]
               (doall
                (cons
                 {:fx/type :tree-item
                  :expanded true
                  :value (fx/sub context translate-sub ::open)
                  :children (for [file (vals open)]
                              {:fx/type :tree-item :value file})}
                 (for [path not-open]
                   {:fx/type :tree-item
                    :value {:path path :type resource-type}}))))})

(defn file-list
  [{:keys [fx/context]}]
  {:fx/type :v-box
   :children [{:fx/type :label
               :text (fx/sub context translate-sub ::label)
               :style-class ["app-title" "label"]}
              {:fx/type fx.ext.tree-view/with-selection-props
               :v-box/vgrow :always
               :props {:selection-mode :single
                       :on-selected-item-changed {::events/type ::events/file-selection-changed}}
               :desc {:fx/type :tree-view
                      :show-root false
                      ;; TODO possible feature request
                      ;; It seems cell factories can't rely on cljfx context
                      ;; Otherwise, the values would be keywords (to be translated with translate-sub)
                      ;; Dev seems to allude to this here: https://github.com/cljfx/cljfx#factory-props
                      :cell-factory {:fx/cell-type :tree-cell
                                     :describe (fn [value]
                                                 {:style-class ["cell" "indexed-cell" "tree-cell" "app-file-list-cell"]
                                                  :text (if (string? value)
                                                          value
                                                          (fs/base-name (:path value) true))})}
                      :context-menu {:fx/type context-menu}
                      :on-mouse-clicked {::events/type ::events/file-list-click}
                      :on-key-pressed {::events/type ::events/file-list-keypress}
                      :root {:fx/type :tree-item
                             :children (doall
                                        (for [group resource-types]
                                          {:fx/type file-group
                                           :resource-type group}))}}}]})
