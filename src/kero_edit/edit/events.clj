(ns kero-edit.edit.events
  (:require [cljfx.api :as fx])
  (:import [javafx.scene.control Dialog DialogEvent ButtonType ButtonBar$ButtonData]))

(defmulti event-handler ::type)

(defmethod event-handler ::notepad-text-changed [{:keys [fx/event fx/context]}]
  {:context (fx/swap-context context assoc :notepad-text event)})

(defmethod event-handler ::license-dialog-consumed [{:keys [fx/event fx/context]}]
  {:context (fx/swap-context context assoc :license-accepted
                             (= ButtonBar$ButtonData/YES (.getButtonData ^ButtonType (.getResult ^Dialog (.getSource event)))))})
