(ns cf.studio.events
  (:require [cf.kero.field.pxpack :as pxpack]
            [cf.kero.metadata :as metadata]
            [cf.kero.util :as util]
            [cf.studio.effects :as effects]
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

#_(defmethod event-handler ::notepad-text-changed [{:keys [fx/event fx/context]}]
  {:context (fx/swap-context context assoc :notepad-text event)})

#_(defmethod event-handler ::displayed-layers-changed [{:keys [fx/event fx/context layer]}]
  {:context (fx/swap-context context update :displayed-layers #(if event (conj % layer) (disj % layer)))})

#_(defmethod event-handler ::selected-layer-changed [{:keys [fx/context layer]}]
  {:context (fx/swap-context context assoc :selected-layer layer)})

#_(defmethod event-handler ::draw-mode-changed [{:keys [fx/context mode]}]
  {:context (fx/swap-context context assoc :draw-mode mode)})

#_(defmethod event-handler ::view-toggles-changed [{:keys [fx/event fx/context view]}]
  {:context (fx/swap-context context update :view-toggles #(if event (conj % view) (disj % view)))})

#_(defmethod event-handler ::edit-mode-changed [{:keys [fx/context mode]}]
  {:context (fx/swap-context context assoc :edit-mode mode)})

;; These events relate to opening and loading a new mod project

;; FIXME Choose file effect occurs before close mod event. May need feature request from cljfx
(defmethod event-handler ::open-new-mod
  [{:keys [fx/context]}]
  {:dispatch {::type ::close-mod}
   ::effects/choose-file {:title (fx/sub context translate-sub ::open-new-mod-chooser-title)
                          :initial-directory (if-let [last-path (fx/sub context :last-executable-path)]
                                               (fs/parent last-path)
                                               (fs/home))
                          :extension-filters [{:description (fx/sub context translate-sub ::open-new-mod-filter-description)
                                               :extensions ["*.exe"]}]
                          :dialog-type :open
                          :on-complete {::type ::open-mod}}})

(defmethod event-handler ::open-last-mod
  [{:keys [fx/context]}]
  [[:dispatch {::type ::close-mod}]
   [:dispatch {::type ::open-mod :path (fx/sub context :last-executable-path)}]])

(defmethod event-handler ::open-mod
  [{:keys [fx/context path]}]
  {:context (fx/swap-context context assoc :last-executable-path (str path))
   ::effects/read-file {:file {:path path}
                        :reader-fn metadata/executable->metadata
                        :on-complete {::type ::load-mod}
                        :on-exception {::type ::exception}}})

(defmethod event-handler ::load-mod
  [{:keys [fx/context] {:keys [data]} :file}]
  {:context (fx/swap-context context assoc :metadata data)})

;; TODO Create prompt effect
;; Checks for unsaved work and only clears everything if there is none or user still wants to close
(defmethod event-handler ::close-mod
  [{:keys [fx/context]}]
  (concat
   (for [[_ editor] (fx/sub context :editors)]
     ;; editor contains the necessary path & type fields for a file
     ;; TODO actually it uses non-namespaced type kw
     [:dispatch {::type ::close-editor :file editor}])
   {:dispatch {::type ::clear-mod}}))

(defmethod event-handler ::clear-mod
  [{:keys [fx/context]}]
  {:context (fx/swap-context context dissoc :metadata :selected-file :open-files :editors :current-editor)})

;; These events relate to actions done with files in the tree list view

(defmethod event-handler ::file-selection-changed
  [{:keys [fx/context ^TreeItem fx/event]}]
  ;; event is nil if moving from child to parent with left arrow key (bug?)
  (let [value (some-> event .getValue)]
    (if (or (nil? value) (string? value))
      {:context (fx/swap-context context dissoc :selected-file)}
      {:context (fx/swap-context context assoc :selected-file value)})))

(defmethod event-handler ::file-list-click
  [{:keys [fx/context ^MouseEvent fx/event]}]
  (when-let [file (fx/sub context :selected-file)]
    (when (and (= MouseButton/PRIMARY (.getButton event))
               (= 2 (.getClickCount event)))
      {:dispatch {::type ::open-file :file file :create-editor? true}})))

;; TODO
;; Enter key accelerator doesn't work on the corresponding menu item so we have this
;; Figure out why enter doesn't work and if we can get rid of this redundancy
(defmethod event-handler ::file-list-keypress
  [{:keys [fx/context ^KeyEvent fx/event]}]
  (when-let [file (fx/sub context :selected-file)]
    (when (= KeyCode/ENTER (.getCode event))
      {:dispatch {::type ::open-file :file file :create-editor? true}})))

;; These events relate to reading files

(defmethod event-handler ::open-file
  [{:keys [fx/context create-editor?] {:keys [path type] :as file} :file}]
  ;; File may already be open (e.g. loaded as asset for another file)
  (if (get-in (fx/sub context :open-files) [type path])
    (when create-editor? {:dispatch {::type ::create-editor :file file}})
    {::effects/read-file {:file file
                          :reader-fn (partial util/decode-file (metadata/resource-type->codec type))
                          :on-complete {::type ::load-file :create-editor? create-editor?}
                          :on-exception {::type ::exception}}}))

(defmethod event-handler ::load-file
  [{:keys [fx/context create-editor?] {:keys [path type] :as file} :file}]
  (merge
   {:context (fx/swap-context context assoc-in [:open-files type path] file)}
   (when create-editor? {:dispatch {::type ::create-editor :file file}})))

(defmethod event-handler ::close-file
  [{:keys [fx/context] {:keys [path type]} :file}]
  {:context (fx/swap-context context update-in [:open-files type] dissoc path)})

;; These events relate to managing editors

(defmethod event-handler ::create-editor
  [{:keys [fx/context] {:keys [path data type]} :file}]
  (let [editor-path (if (not= type ::pxpack/pxpack) [path] [path ::pxpack/head])
        ;; Default editor, needs to be adjusted if editor is for a field
        base-editor {:type type :path path :edits [data] :edit-pos 0 :dirty false}]
    (merge
     {:dispatch {::type ::switch-to-editor :editor-path editor-path}}
     ;; If the editor does not already exist, create it
     (when-not (get (fx/sub context :editors) path)
       (let [editor (if (not= type ::pxpack/pxpack)
                      base-editor
                      ;; Field editor is really 3 separate editors for head, tiles, entities
                      (reduce
                       (fn [editor field-section]
                         ;; Insert subeditors into wrapping editor
                           (assoc editor
                                  field-section
                                  (assoc base-editor
                                         :type field-section
                                         :edits [(field-section data)])))
                       {:type ::pxpack/pxpack :path path :dirty false}
                       [::pxpack/head ::pxpack/tile-layers ::pxpack/units]))]
         {:context (fx/swap-context context assoc-in [:editors path] editor)})))))

(defmethod event-handler ::switch-to-editor
  [{:keys [fx/context editor-path]}]
  {:context (fx/swap-context context assoc :current-editor editor-path)})

;; TODO Use prompt effect (see todo on ::close-mod)
(defmethod event-handler ::close-editor
  [{:keys [fx/context] {:keys [path] :as file} :file}]
  (concat
   {:dispatch {::type ::close-file :file file}}
   {:context (fx/swap-context context update :editors dissoc path)}
   (when (= path (first (fx/sub context :current-editor)))
     {:context (fx/swap-context context dissoc :current-editor)})))
