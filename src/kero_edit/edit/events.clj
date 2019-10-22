(ns kero-edit.edit.events
  (:require [clojure.stacktrace :refer [print-cause-trace]]
            [me.raynes.fs :as fs]
            [cljfx.api :as fx]
            [kero-edit.kero.gamedata :as gamedata]
            [kero-edit.edit.i18n :refer [translate-sub]])
  (:import [java.util Collection List]
           [java.io IOException]
           [javafx.event Event]
           [javafx.scene.control Dialog DialogEvent ButtonType ButtonBar$ButtonData]
           [javafx.stage Stage FileChooser FileChooser$ExtensionFilter]))

(defn choose-file-effect [{:keys [title initial-directory initial-filename extension-filters dialog-type on-complete]} dispatch!]
  (fx/on-fx-thread
   (let [chooser (doto (FileChooser.)
                   (.setTitle title)
                   (.setInitialDirectory initial-directory)
                   (.setInitialFileName initial-filename)
                   (->
                    (.getExtensionFilters)
                    (.addAll ^Collection (map
                                          #(FileChooser$ExtensionFilter. ^String (:description %) ^List (:extensions %))
                                          extension-filters))))]
     (dispatch! (merge on-complete
                       (case dialog-type
                         ;; TODO Grab primary stage to optionally allow blocking modality
                         :open {:path (.showOpenDialog chooser (Stage.))}
                         :open-multiple {:paths (.showOpenMultipleDialog chooser (Stage.))}
                         :save {:path (.showSaveDialog chooser (Stage.))}))))))

(defn read-file-effect [{:keys [path reader-fn on-complete on-exception]} dispatch!]
  (try
    (dispatch! (assoc on-complete :path path :data (reader-fn path)))
    (catch IOException e (dispatch! (assoc on-exception :path path :exception e)))))

(defn write-file-effect [{:keys [path writer-fn data on-complete on-exception]} dispatch!]
  (try
    (writer-fn path)
    (dispatch! (assoc on-complete :path path))
    (catch IOException e (dispatch! (assoc on-exception :path path :exception e)))))

(defn shutdown-effect [{:keys [fx/context]} _]
  ;; TODO Invoke config/write-config
  (javafx.application.Platform/exit)
  (shutdown-agents))

(defn exception-dialog-effect [{:keys [fx/context ^Exception exception]} _]
  (fx/on-fx-thread
   (fx/create-component
    {:fx/type :dialog
     :title (fx/sub context translate-sub ::exception-dialog-title)
     :showing true
     :dialog-pane {:fx/type :dialog-pane
                   :button-types [:ok]
                   :expanded true
                   :expandable-content {:fx/type :v-box
                                        :spacing 20
                                        :alignment :center-left
                                        :children [{:fx/type :text
                                                    :text (.getLocalizedMessage exception)
                                                    :font {:family "" :size 15}}
                                                   {:fx/type :text-area
                                                    :editable false
                                                    :text (with-out-str (print-cause-trace exception))
                                                    :v-box/vgrow :always}]}}})))

(defmulti event-handler ::type)

(defmethod event-handler ::exception [event-map] {:exception-dialog event-map})

(defmethod event-handler ::shutdown [_] {:shutdown {}})

(defmethod event-handler ::notepad-text-changed [{:keys [fx/event fx/context]}]
  {:context (fx/swap-context context assoc :notepad-text event)})

(defmethod event-handler ::license-dialog-consumed [{:keys [^Event fx/event fx/context]}]
  (let [accepted (= ButtonBar$ButtonData/YES (.getButtonData ^ButtonType (.getResult ^Dialog (.getSource event))))]
    (merge {:context (fx/swap-context context assoc :license-accepted accepted)}
           (if-not accepted {:dispatch {::type ::shutdown}}))))

(defmethod event-handler ::open-new-mod [{:keys [fx/context]}]
  {:context (fx/swap-context context dissoc :gamedata)
   :choose-file {:title (fx/sub context translate-sub ::open-new-mod-chooser-title)
                 :initial-directory (if-let [last-path (fx/sub context :last-executable-path)] (fs/parent last-path) (fs/home))
                 :extension-filters [{:description (fx/sub context translate-sub ::open-new-mod-filter-description)
                                      :extensions ["*.exe"]}]
                 :dialog-type :open
                 :on-complete {::type ::open-mod}}})

(defmethod event-handler ::open-last-mod [{:keys [fx/context]}]
  {:context (fx/swap-context context dissoc :gamedata)
   :dispatch {::type ::open-mod :path (fx/sub context :last-executable-path)}})

(defmethod event-handler ::open-mod [{:keys [fx/context path]}]
  {:context (fx/swap-context context assoc :last-executable-path path)
   :read-file {:path path
               :reader-fn gamedata/executable->gamedata
               :on-complete {::type ::load-gamedata}
               :on-exception {::type ::exception}}})

(defmethod event-handler ::load-gamedata [{:keys [fx/context data]}]
  {:context (fx/swap-context context assoc :gamedata data)})
