(ns kero-edit.edit.effects
  (:require [clojure.stacktrace :refer [print-cause-trace]]
            [cljfx.api :as fx]
            [kero-edit.edit.i18n :refer [translate-sub]])
  (:import [java.util Collection List]
           [java.io IOException]
           [javafx.stage Stage FileChooser FileChooser$ExtensionFilter]))

(defn choose-file [{:keys [title initial-directory initial-filename extension-filters dialog-type on-complete]} dispatch!]
  (fx/on-fx-thread
   (let [chooser (doto (FileChooser.)
                   (.setTitle title)
                   (.setInitialDirectory initial-directory)
                   (.setInitialFileName initial-filename)
                   (->
                    (.getExtensionFilters)
                    (.addAll ^Collection (map
                                          #(FileChooser$ExtensionFilter. ^String (:description %) ^List (:extensions %))
                                          extension-filters))))
         selected-path (case dialog-type
                         ;; TODO Grab primary stage to optionally allow blocking modality
                         :open {:path (.showOpenDialog chooser (Stage.))}
                         :open-multiple {:paths (.showOpenMultipleDialog chooser (Stage.))}
                         :save {:path (.showSaveDialog chooser (Stage.))})]
     ;; If user escapes dialog w/o making a choice, path is nil
     (if (first (vals selected-path))
       (dispatch! (merge on-complete selected-path))))))

(defn read-file [{:keys [path reader-fn on-complete on-exception]} dispatch!]
  (try
    (dispatch! (assoc on-complete :file {:path path :data (reader-fn path)}))
    (catch IOException e (dispatch! (assoc on-exception :path path :exception e)))))

(defn write-file [{:keys [path writer-fn data on-complete on-exception]} dispatch!]
  (try
    (writer-fn path)
    (dispatch! (assoc on-complete :path path))
    (catch IOException e (dispatch! (assoc on-exception :path path :exception e)))))

(defn exception-dialog [{:keys [fx/context ^Exception exception]} _]
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

(defn shutdown [{:keys [fx/context]} _]
  ;; TODO Invoke config/write-config
  (javafx.application.Platform/exit)
  (shutdown-agents))
