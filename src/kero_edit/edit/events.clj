(ns kero-edit.edit.events
  (:require [me.raynes.fs :as fs]
            [cljfx.api :as fx]
            [kero-edit.kero.gamedata :as gamedata]
            [kero-edit.edit.i18n :refer [translate-sub]]
            [kero-edit.edit.effects :as effects])
  (:import [javafx.event Event]
           [javafx.scene.control Dialog DialogEvent ButtonType ButtonBar$ButtonData]))

(defmulti event-handler ::type)

(defmethod event-handler ::exception [event-map] {::effects/exception-dialog event-map})

(defmethod event-handler ::shutdown [_] {::effects/shutdown {}})

(defmethod event-handler ::notepad-text-changed [{:keys [fx/event fx/context]}]
  {:context (fx/swap-context context assoc :notepad-text event)})

(defmethod event-handler ::license-dialog-consumed [{:keys [^Event fx/event fx/context]}]
  (let [accepted (= ButtonBar$ButtonData/YES (.getButtonData ^ButtonType (.getResult ^Dialog (.getSource event))))]
    (merge {:context (fx/swap-context context assoc :license-accepted accepted)}
           (if-not accepted {:dispatch {::type ::shutdown}}))))

(defmethod event-handler ::open-new-mod [{:keys [fx/context]}]
  {:context (fx/swap-context context dissoc :gamedata)
   ::effects/choose-file {:title (fx/sub context translate-sub ::open-new-mod-chooser-title)
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
   ::effects/read-file {:path path
                        :reader-fn gamedata/executable->gamedata
                        :on-complete {::type ::load-gamedata}
                        :on-exception {::type ::exception}}})

(defmethod event-handler ::load-gamedata [{:keys [fx/context data]}]
  {:context (fx/swap-context context assoc :gamedata data)})
