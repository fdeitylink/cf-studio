(ns cf.studio.events.core
  (:require [cf.kero.field.pxpack :as pxpack]
            [cf.kero.game-data :as game-data]
            [cf.studio.effects :as effects]
            [cf.studio.file-graph :as file-graph]
            [cf.studio.i18n :refer [translate-sub]]
            [cljfx.api :as fx]
            [me.raynes.fs :as fs])
  (:import javafx.event.Event
           [javafx.scene.control ButtonBar$ButtonData ButtonType Dialog TreeItem]
           [javafx.scene.input KeyCode KeyEvent MouseButton MouseEvent]))

(defmulti event-handler ::type)

;; These events simply delegate to effects
;; Used when a function or property expects an event but an effect is needed

(defmethod event-handler ::exception
  [event-map]
  {::effects/exception-dialog event-map})

(defmethod event-handler ::shutdown
  [event-map]
  {::effects/shutdown event-map})

(defmethod event-handler ::license-dialog-consumed
  [{:keys [^Event fx/event fx/context]}]
  (let [accepted (= ButtonBar$ButtonData/YES
                    (-> event ^Dialog (.getSource) ^ButtonType (.getResult) .getButtonData))]
    (merge
     {:context (fx/swap-context context assoc :license-accepted accepted)}
     (when-not accepted {:dispatch {::type ::shutdown}}))))

;; These events relate to opening and loading a new mod project

;; FIXME Choose file effect occurs before close mod event. May need feature request from cljfx
(defmethod event-handler ::open-new-mod
  [{:keys [fx/context]}]
  {:dispatch {::type ::close-mod}
   ::effects/choose-file {:title (fx/sub-ctx context translate-sub ::open-new-mod-chooser-title)
                          :initial-directory (if-let [last-path (fx/sub-val context :last-executable-path)]
                                               (fs/parent last-path)
                                               (fs/home))
                          :extension-filters [{:description (fx/sub-ctx context translate-sub ::open-new-mod-filter-description)
                                               :extensions ["*.exe"]}]
                          :dialog-type :open
                          :on-complete {::type ::open-mod}}})

(defmethod event-handler ::open-last-mod
  [{:keys [fx/context]}]
  [[:dispatch {::type ::close-mod}]
   [:dispatch {::type ::open-mod :path (fx/sub-val context :last-executable-path)}]])

(defmethod event-handler ::open-mod
  [{:keys [fx/context path]}]
  {:context (fx/swap-context context assoc :last-executable-path (str path))
   ::effects/read-file {:file {:path path}
                        :reader-fn game-data/executable->game-data
                        :on-complete {::type ::load-mod}
                        :on-exception {::type ::exception}}})

(defmethod event-handler ::load-mod
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
(defmethod event-handler ::close-mod
  [{:keys [fx/context]}]
  (concat
   (for [path (file-graph/paths (fx/sub-ctx context file-graph/filter-editing-files-sub))]
     [:dispatch {::type ::close-editor :path path}])
   {:dispatch {::type ::clear-mod}}))

(defmethod event-handler ::clear-mod
  [{:keys [fx/context]}]
  {:context (fx/swap-context context #(-> %
                                          (dissoc :game-data :selected-path :editor)
                                          (assoc :files (file-graph/file-graph))))})

;; These events relate to actions done with files in the tree list view

(defmethod event-handler ::selected-path-changed
  [{:keys [fx/context ^TreeItem fx/event]}]
  ;; event is nil if moving from child to parent with left arrow key (bug?)
  (let [value (some-> event .getValue)]
    (if (or (nil? value) (string? value))
      {:context (fx/swap-context context dissoc :selected-path)}
      {:context (fx/swap-context context assoc :selected-path value)})))

(defmethod event-handler ::file-list-click
  [{:keys [^MouseEvent fx/event]}]
  (when (and (= MouseButton/PRIMARY (.getButton event))
             (= 2 (.getClickCount event)))
    {:dispatch {::type ::open-selected-path}}))

;; TODO
;; Enter key accelerator doesn't work on the corresponding menu item so we have this
;; Figure out why and if we can just use an on-action on the menu item
(defmethod event-handler ::file-list-keypress
  [{:keys [^KeyEvent fx/event]}]
  (when (= KeyCode/ENTER (.getCode event))
    {:dispatch {::type ::open-selected-path}}))

(defmethod event-handler ::open-selected-path
  [{:keys [fx/context]}]
  (when-let [path (fx/sub-val context :selected-path)]
    {:dispatch {::type ::open-file :path path :edit? true}
     :context (fx/swap-context context dissoc :selected-path)}))

;; These events relate to reading files

(defmethod event-handler ::open-file
  [{:keys [fx/context path edit?]}]
  (let [file (fx/sub-ctx context file-graph/file-sub path)]
    (if (fx/sub-ctx context file-graph/is-file-open?-sub path)
      (when edit? {:dispatch {::type ::init-editor :file file}})
      {::effects/read-file {:file file
                            :reader-fn (game-data/resource-type->reader-fn (:type file))
                            :on-complete {::type ::load-file :edit? edit?}
                            :on-exception {::type ::exception}}})))

(defmethod event-handler ::load-file
  [{:keys [fx/context edit?] {:keys [path data] :as file} :file}]
  (merge
   {:context (fx/swap-context context update :files file-graph/open-file path data)}
   (when edit? {:dispatch {::type ::init-editor :file file}})))

;; These events relate to managing editors

(defmethod event-handler ::init-editor
  [{:keys [fx/context] {:keys [path type] :as file} :file}]
  (concat
   [[:context (fx/swap-context context update :files file-graph/open-editor path)]
    [:dispatch {::type (case type
                         ::pxpack/pxpack ::pxpack-init-editor)
                :file file}]
    [:dispatch {::type ::switch-editor :path path}]]))

(defmethod event-handler ::load-dependencies
  [{:keys [fx/context path dependencies]}]
  (let [dep-paths (map
                   ;; TODO Handle localize items
                   (fn [[dep-name type]]
                     (let [resource-dir (fx/sub-val context get-in [:game-data ::game-data/resource-dir])
                           {:keys [subdir extension]} (type game-data/resource-type->path-meta)]
                       (fs/file resource-dir subdir (str dep-name extension))))
                   dependencies)]
    (cons
     [:context (fx/swap-context context update :files file-graph/add-file-dependencies path dep-paths)]
     (for [dep-path dep-paths]
       [:dispatch {::type ::open-file :path dep-path :edit? false}]))))

(defmethod event-handler ::switch-editor
  [{:keys [fx/context path]}]
  {:context (fx/swap-context context assoc :editor path)})

;; TODO Use prompt effect (see todo on ::close-mod)
(defmethod event-handler ::close-editor
  [{:keys [fx/context path]}]
  {:context (fx/swap-context context
                             #(as-> % ctxt
                                (update ctxt :files file-graph/close-editor path)
                                (if (= path (:editor ctxt))
                                  (dissoc ctxt :editor)
                                  ctxt)))})

(load "pxpack_events")
