(ns kero-edit.edit.settings-view
  (:require [cljfx.api :as fx]
            [kero-edit.kero.field.tile-layer :as tile-layer]
            [kero-edit.edit.i18n :refer [translate-sub]]
            [kero-edit.edit.events :as events])
  (:import [javafx.scene.text Font FontWeight]))

(def ^:private font (Font/font "" FontWeight/BOLD 15.0))

(defn settings-view
  [{:keys [fx/context]}]
  {:fx/type fx/ext-let-refs
   :refs {:layer-tg {:fx/type :toggle-group}
          :draw-mode-tg {:fx/type :toggle-group}}
   :desc {:fx/type :grid-pane
          :padding 10
          :vgap 10
          :hgap 20
          :children (concat
                     [{:fx/type :label
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
                       :grid-pane/column 4}]

                     (flatten
                      (for [[layer row] (map vector tile-layer/layers (range 1 4))]
                        [{:fx/type :check-box
                          :text (fx/sub context translate-sub (keyword "kero-edit.edit.settings-view" (name layer)))
                          :font font
                          :selected (contains? (fx/sub context :displayed-layers) layer)
                          :on-selected-changed {::events/type ::events/displayed-layers-changed :layer layer}
                          :grid-pane/row row
                          :grid-pane/column 0}
                         {:fx/type :radio-button
                          :text (fx/sub context translate-sub (keyword "kero-edit.edit.settings-view" (name layer)))
                          :font font
                          :toggle-group {:fx/type fx/ext-get-ref :ref :layer-tg}
                          :selected (= (fx/sub context :selected-layer) layer)
                          :on-selected-changed {::events/type ::events/selected-layer-changed :layer layer}
                          :grid-pane/row row
                          :grid-pane/column 1}]))

                     (doall
                      (for [[mode row] (map vector [:draw :rect :copy :fill :replace] (range 1 6))]
                        {:fx/type :radio-button
                         :text (fx/sub context translate-sub (keyword "kero-edit.edit.settings-view" (name mode)))
                         :font font
                         :toggle-group {:fx/type fx/ext-get-ref :ref :draw-mode-tg}
                         :selected (= (fx/sub context :draw-mode) mode)
                         :on-selected-changed {::events/type ::events/draw-mode-changed :mode mode}
                         :grid-pane/row row
                         :grid-pane/column 2})))}})
