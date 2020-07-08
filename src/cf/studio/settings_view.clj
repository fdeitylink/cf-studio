(ns cf.studio.settings-view
  (:require [cf.kero.field.tile-layer :as tile-layer]
            [cf.studio.events :as events]
            [cf.studio.i18n :refer [translate-sub]]
            [cljfx.api :as fx])
  (:import [javafx.scene.text Font FontWeight]))

;; TODO Add to let-refs
(def ^:private font (Font/font "" FontWeight/BOLD 15.0))

(defn settings-view
  [{:keys [fx/context]}]
  {:fx/type fx/ext-let-refs
   :refs (zipmap
          [:layer-tg :draw-mode-tg :edit-mode-tg]
          (repeat {:fx/type :toggle-group}))
   :desc {:fx/type :titled-pane
          :text (fx/sub context translate-sub ::title)
          :animated false
          ;; TODO store expanded property in config
          :content {:fx/type :grid-pane
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
                                    :text (fx/sub context translate-sub (keyword "cf.studio.settings-view" (name layer)))
                                    :font font
                                    :selected (contains? (fx/sub context :displayed-layers) layer)
                                    :on-selected-changed {::events/type ::events/displayed-layers-changed :layer layer}
                                    :grid-pane/row row
                                    :grid-pane/column 0}
                                   {:fx/type :radio-button
                                    :text (fx/sub context translate-sub (keyword "cf.studio.settings-view" (name layer)))
                                    :font font
                                    :toggle-group {:fx/type fx/ext-get-ref :ref :layer-tg}
                                    :selected (= (fx/sub context :selected-layer) layer)
                                    :on-selected-changed {::events/type ::events/selected-layer-changed :layer layer}
                                    :grid-pane/row row
                                    :grid-pane/column 1}]))

                               (doall
                                (for [[mode row] (map vector [:draw :rect :copy :fill :replace] (range 1 6))]
                                  {:fx/type :radio-button
                                   :text (fx/sub context translate-sub (keyword "cf.studio.settings-view" (name mode)))
                                   :font font
                                   :toggle-group {:fx/type fx/ext-get-ref :ref :draw-mode-tg}
                                   :selected (= (fx/sub context :draw-mode) mode)
                                   :on-selected-changed {::events/type ::events/draw-mode-changed :mode mode}
                                   :grid-pane/row row
                                   :grid-pane/column 2}))

                               (doall
                                (for [[view row] (map vector [:tile-types :grid :entity-boxes :entity-sprites :entity-names] (range 1 6))]
                                  {:fx/type :check-box
                                   :text (fx/sub context translate-sub (keyword "cf.studio.settings-view" (name view)))
                                   :font font
                                   :selected (contains? (fx/sub context :view-toggles) view)
                                   :on-selected-changed {::events/type ::events/view-toggles-changed :view view}
                                   :grid-pane/row row
                                   :grid-pane/column 3}))

                               (doall
                                (for [[mode row] (map vector [:tile :entity] (range 1 3))]
                                  {:fx/type :radio-button
                                   :text (fx/sub context translate-sub (keyword "cf.studio.settings-view" (name mode)))
                                   :font font
                                   :toggle-group {:fx/type fx/ext-get-ref :ref :edit-mode-tg}
                                   :selected (= (fx/sub context :edit-mode) mode)
                                   :on-selected-changed {::events/type ::events/edit-mode-changed :mode mode}
                                   :grid-pane/row row
                                   :grid-pane/column 4})))}}})
