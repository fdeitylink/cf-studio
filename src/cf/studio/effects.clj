(ns cf.studio.effects
  (:require [cf.studio.config :as config]
            [cf.studio.i18n :refer [translate-sub]]
            [cf.util :as util]
            [cljfx.api :as fx]
            [clojure.stacktrace :refer [print-cause-trace]]
            [me.raynes.fs :as fs])
  (:import java.io.IOException
           [java.util Collection List]
           [javafx.stage FileChooser FileChooser$ExtensionFilter Stage]))

(defn choose-file [{:keys [title initial-directory initial-filename extension-filters dialog-type on-complete]} dispatch!]
  (let [chooser (doto (FileChooser.)
                  (.setTitle title)
                  (.setInitialDirectory (if (fs/directory? initial-directory) initial-directory (fs/home)))
                  (.setInitialFileName initial-filename)
                  (->
                   (.getExtensionFilters)
                   (.addAll ^Collection (map
                                         #(FileChooser$ExtensionFilter. ^String (:description %) ^List (:extensions %))
                                         extension-filters))))
        selection (case dialog-type
                    ;; TODO Grab primary stage to optionally allow blocking modality
                    :open {:path @(fx/on-fx-thread (.showOpenDialog chooser (Stage.)))}
                    :open-multiple {:paths @(fx/on-fx-thread (.showOpenMultipleDialog chooser (Stage.)))}
                    :save {:path @(fx/on-fx-thread (.showSaveDialog chooser (Stage.)))})]
     ;; If user escapes dialog w/o making a choice, path is nil
    (when (first (vals selection))
      (dispatch! (merge on-complete selection)))))

(defn read-file [{:keys [file reader-fn on-complete on-exception]} dispatch!]
  (dispatch!
    (try
      (assoc on-complete :file (assoc file :data (reader-fn (:path file))))
      (catch IOException e (assoc on-exception :file file :exception e)))))

(defn write-file [{:keys [file writer-fn on-complete on-exception]} dispatch!]
  (dispatch!
   (try
     (assoc on-complete :file file :result (writer-fn (:path file) (:data file)))
     (catch IOException e (assoc on-exception :file file :exception e)))))

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
                                                    :text (.getLocalizedMessage exception)}
                                                   {:fx/type :text-area
                                                    :editable false
                                                    :text (with-out-str (print-cause-trace exception))
                                                    :v-box/vgrow :always}]}}})))

(defn shutdown [{:keys [fx/context]} _]
  (config/write-config! (config/context->config context))
  (when-not util/running-in-repl?
    (javafx.application.Platform/exit)
    (shutdown-agents)))
