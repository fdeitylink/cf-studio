(ns kero-edit.edit.settings-grid
  (:require [cljfx.api :as fx]
            [kero-edit.edit.i18n :refer [sub-translate]])
  (:import [javafx.scene.text Font FontWeight]))

(def ^:private font (Font/font "" FontWeight/BOLD 15.0))

(defn settings-grid
  [{:keys [fx/context]}]
  {:fx/type :grid-pane
   :padding 10
   :vgap 10
   :hgap 20
   :children [{:fx/type :label
               :text (fx/sub context sub-translate ::displayed-layers)
               :font font
               :grid-pane/row 0
               :grid-pane/column 0}

              {:fx/type :label
               :text (fx/sub context sub-translate ::selected-layer)
               :font font
               :grid-pane/row 0
               :grid-pane/column 1}

              {:fx/type :label
               :text (fx/sub context sub-translate ::draw-mode)
               :font font
               :grid-pane/row 0
               :grid-pane/column 2}

              {:fx/type :label
               :text (fx/sub context sub-translate ::view-toggles)
               :font font
               :grid-pane/row 0
               :grid-pane/column 3}

              {:fx/type :label
               :text (fx/sub context sub-translate ::edit-mode)
               :font font
               :grid-pane/row 0
               :grid-pane/column 4}]})
