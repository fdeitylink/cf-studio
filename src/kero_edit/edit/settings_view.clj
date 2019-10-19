(ns kero-edit.edit.settings-view
  (:require [cljfx.api :as fx]
            [kero-edit.edit.i18n :refer [translate-sub]])
  (:import [javafx.scene.text Font FontWeight]))

(def ^:private font (Font/font "" FontWeight/BOLD 15.0))

(defn settings-view
  [{:keys [fx/context]}]
  {:fx/type :grid-pane
   :padding 10
   :vgap 10
   :hgap 20
   :children [{:fx/type :label
               :text (fx/sub context translate-sub ::displayed-layers)
               :font font
               :grid-pane/row 0
               :grid-pane/column 0}

              {:fx/type :label
               :text (fx/sub context translate-sub ::selected-layer)
               :font font
               :grid-pane/row 0
               :grid-pane/column 1}

              {:fx/type :label
               :text (fx/sub context translate-sub ::draw-mode)
               :font font
               :grid-pane/row 0
               :grid-pane/column 2}

              {:fx/type :label
               :text (fx/sub context translate-sub ::view-toggles)
               :font font
               :grid-pane/row 0
               :grid-pane/column 3}

              {:fx/type :label
               :text (fx/sub context translate-sub ::edit-mode)
               :font font
               :grid-pane/row 0
               :grid-pane/column 4}]})
