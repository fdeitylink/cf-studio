(ns cf.studio.editors.editor-view
  (:require [cf.kero.field.pxpack :as pxpack]
            [cf.studio.editors.pxpack-editor :refer [pxpack-editor]]
            [cf.studio.i18n :refer [translate-sub]]
            [cljfx.api :as fx]))

(defn editor-view
  [{:keys [fx/context]}]
  (let [editor (get-in
                (fx/sub context :editors)
                (fx/sub context :current-editor))]
    (case (:type editor)
      ::pxpack/head {:fx/type pxpack-editor :editor editor}
      {:fx/type :stack-pane
       :children [{:fx/type :text
                   :stack-pane/alignment :center
                   :stack-pane/margin 10
                   :text (fx/sub context translate-sub ::no-editor-open)
                   :font {:family "" :size 15}}]})))
