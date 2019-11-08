(ns kero-edit.edit.events
  (:require [flatland.ordered.map :refer [ordered-map]]
            [me.raynes.fs :as fs]
            [cljfx.api :as fx]
            [kero-edit.kero.field.pxpack :as pxpack]
            [kero-edit.kero.gamedata :as gamedata]
            [kero-edit.kero.util :as util]
            [kero-edit.edit.config :as config]
            [kero-edit.edit.i18n :refer [translate-sub]]
            [kero-edit.edit.effects :as effects])
  (:import [javafx.event Event]
           [javafx.scene.input MouseButton MouseEvent KeyCode KeyEvent]
           [javafx.scene.control Dialog DialogEvent ButtonType ButtonBar$ButtonData]))

(defmulti event-handler ::type)

;; These events simply delegate to effects - for when a function or property expects an event but an effect needs to occur

(defmethod event-handler ::exception [event-map] {::effects/exception-dialog event-map})

(defmethod event-handler ::shutdown [{:keys [fx/context]}]
  {::effects/shutdown {:config-path (fx/sub context :config-path)
                       ;; Filter for items in context that are in the config
                       :config (into {} (filter (fn [[k _]] (contains? config/default-config k))
                                                (:cljfx.context/m context)))}})

(defmethod event-handler ::license-dialog-consumed [{:keys [^Event fx/event fx/context]}]
  (let [accepted (= ButtonBar$ButtonData/YES (.getButtonData ^ButtonType (.getResult ^Dialog (.getSource event))))]
    (merge {:context (fx/swap-context context assoc :license-accepted accepted)}
           (if-not accepted {:dispatch {::type ::shutdown}}))))

(defmethod event-handler ::notepad-text-changed [{:keys [fx/event fx/context]}]
  {:context (fx/swap-context context assoc :notepad-text event)})

(defmethod event-handler ::displayed-layers-changed [{:keys [fx/event fx/context layer]}]
  {:context (fx/swap-context context update :displayed-layers #(if event (conj % layer) (disj % layer)))})

(defmethod event-handler ::selected-layer-changed [{:keys [fx/context layer]}]
  {:context (fx/swap-context context assoc :selected-layer layer)})

(defmethod event-handler ::draw-mode-changed [{:keys [fx/context mode]}]
  {:context (fx/swap-context context assoc :draw-mode mode)})

(defmethod event-handler ::view-toggles-changed [{:keys [fx/event fx/context view]}]
  {:context (fx/swap-context context update :view-toggles #(if event (conj % view) (disj % view)))})

(defmethod event-handler ::edit-mode-changed [{:keys [fx/context mode]}]
  {:context (fx/swap-context context assoc :edit-mode mode)})

;; This events relate to opening and loading a new mod project

(defmethod event-handler ::open-new-mod [{:keys [fx/context]}]
  {:dispatch {::type ::close-mod}
   ::effects/choose-file {:title (fx/sub context translate-sub ::open-new-mod-chooser-title)
                          :initial-directory (if-let [last-path (fx/sub context :last-executable-path)] (fs/parent last-path) (fs/home))
                          :extension-filters [{:description (fx/sub context translate-sub ::open-new-mod-filter-description)
                                               :extensions ["*.exe"]}]
                          :dialog-type :open
                          :on-complete {::type ::open-mod}}})

(defmethod event-handler ::open-last-mod [{:keys [fx/context]}]
  [[:dispatch {::type ::close-mod}]
   [:dispatch {::type ::open-mod :path (fx/sub context :last-executable-path)}]])

(defmethod event-handler ::open-mod [{:keys [fx/context path]}]
  {:context (fx/swap-context context assoc :last-executable-path (str path))
   ::effects/read-file {:path path
                        :reader-fn gamedata/executable->gamedata
                        :on-complete {::type ::load-gamedata}
                        :on-exception {::type ::exception}}})

(defmethod event-handler ::load-gamedata [{:keys [fx/context file]}]
  {:context (fx/swap-context context assoc :gamedata (:data file))})

(defmethod event-handler ::close-mod [{:keys [fx/context]}]
  {:context (fx/swap-context context #(-> %
                                          (dissoc :gamedata)
                                          (assoc :selected-fields [])
                                          (assoc :loaded-fields (ordered-map))))})

;; These events relate to selecting fields in the list view, opening and loading, and deleting them

(defmethod event-handler ::field-selection-changed [{:keys [fx/context fx/event]}]
  {:context (fx/swap-context context assoc :selected-fields event)})

(defmethod event-handler ::field-list-click [{:keys [fx/context ^MouseEvent fx/event]}]
  (if (and (= MouseButton/PRIMARY (.getButton event))
           (= 2 (.getClickCount event)))
    {:dispatch {::type ::open-fields}}))

(defmethod event-handler ::field-list-keypress [{:keys [fx/context ^KeyEvent fx/event]}]
  (condp = (.getCode event)
    KeyCode/ENTER {:dispatch {::type ::open-fields}}
    ; KeyCode/DELETE {:dispatch {::type ::delete-fields}}
    nil))

(defmethod event-handler ::open-fields [{:keys [fx/context]}]
  (for [field (fx/sub context :selected-fields)]
    [::effects/read-file {:path field
                          :reader-fn #(util/decode-file % pxpack/pxpack-codec)
                          :on-complete {::type ::load-field}
                          :on-exception {::type ::exception}}]))

(defmethod event-handler ::load-field [{:keys [fx/context file]}]
  {:context (fx/swap-context context assoc-in [:loaded-fields (:path file)] file)})

(defmethod event-handler ::close-field [{:keys [fx/context field]}]
  {:context (fx/swap-context context update :loaded-fields #(dissoc % (:path field)))})
