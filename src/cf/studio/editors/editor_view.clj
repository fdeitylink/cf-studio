(ns cf.studio.editors.editor-view
  (:require [cf.kero.field.pxpack :as pxpack]
            [cf.studio.editors.pxpack-editor :refer [pxpack-editor]]
            [cf.studio.i18n :refer [translate-sub]]
            [cljfx.api :as fx]))

(defn- child-editor
  [context]
  (let [editor (get-in
                (fx/sub context :editors)
                (fx/sub context :current-editor))]
    (condp contains? (:type editor)
      #{::pxpack/head ::pxpack/tile-layers ::pxpack/units} {:fx/type pxpack-editor :editor editor}
      {:fx/type :text
       :text (fx/sub context translate-sub ::no-editor-open)
       :style-class "app-text-medium"})))

(defn editor-view
  [{:keys [fx/context]}]
  {:fx/type :stack-pane
   :children [(merge
               (fx/sub context child-editor)
               {:stack-pane/alignment :center
                :stack-pane/margin 10})]})
