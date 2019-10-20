(ns kero-edit.edit.events
  (:require [clojure.stacktrace :refer [print-cause-trace]]
            [cljfx.api :as fx]
            [me.raynes.fs :as fs]
            [kero-edit.kero.gamedata :as gamedata]
            [kero-edit.edit.i18n :refer [translate-sub]])
  (:import [java.io IOException]
           [javafx.event Event]
           [javafx.scene.control Dialog DialogEvent ButtonType ButtonBar$ButtonData]
           [javafx.stage Stage FileChooser FileChooser$ExtensionFilter]))

(defn read-file-effect [{:keys [path reader-fn on-complete on-exception]} dispatch!]
  (try
    (dispatch! (assoc on-complete :path path :data (reader-fn path)))
    (catch IOException e (dispatch! (assoc on-exception :path path :exception e)))))

(defn write-file-effect [{:keys [path writer-fn data on-complete on-exception]} dispatch!]
  (try
    (writer-fn path)
    (dispatch! (assoc on-complete :path path))
    (catch IOException e (dispatch! (assoc on-exception :path path :exception e)))))

(defmulti event-handler ::type)

(defmethod event-handler ::exception [{:keys [fx/context ^Exception exception]}]
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
                                        :alignment :center
                                        :children [{:fx/type :text
                                                    :text (.getLocalizedMessage exception)
                                                    :font {:family "" :size 15}}
                                                   {:fx/type :text-area
                                                    :editable false
                                                    :text (with-out-str (print-cause-trace exception))
                                                    :v-box/vgrow :always}]}}})))

(defmethod event-handler ::notepad-text-changed [{:keys [fx/event fx/context]}]
  {:context (fx/swap-context context assoc :notepad-text event)})

(defmethod event-handler ::license-dialog-consumed [{:keys [^Event fx/event fx/context]}]
  {:context (fx/swap-context context assoc :license-accepted
                             (= ButtonBar$ButtonData/YES (.getButtonData ^ButtonType (.getResult ^Dialog (.getSource event)))))})

(defmethod event-handler ::open-new-mod [{:keys [fx/context]}]
  @(fx/on-fx-thread
    (if-let [path (-> (doto (FileChooser.)
                        (.setTitle (fx/sub context translate-sub ::open-new-mod-chooser-title))
                        (.setInitialDirectory (if-let [last-path (fx/sub context :last-executable-location)] (fs/parent last-path) (fs/home)))
                        (.setSelectedExtensionFilter (FileChooser$ExtensionFilter. ^String (fx/sub context translate-sub ::open-new-mod-filter-description) ["*.exe"])))
                      ;; TODO Grab primary Stage
                      (.showOpenDialog (Stage.)))]
      {:context (fx/swap-context context assoc :last-executable-location path)
       :dispatch {::type ::open-mod :executable-path path}})))

(defmethod event-handler ::open-last-mod [{:keys [fx/context]}]
  {:dispatch {::type ::open-mod :executable-path (fx/sub context :last-executable-location)}})

(defmethod event-handler ::open-mod [{:keys [executable-path]}]
  {:read-file {:path executable-path
               :reader-fn gamedata/executable->gamedata
               :on-complete {::type ::load-gamedata}
               :on-exception {::type ::exception}}})

(defmethod event-handler ::load-gamedata [{:keys [fx/context data]}]
  {:context (fx/swap-context context assoc :gamedata data)})
