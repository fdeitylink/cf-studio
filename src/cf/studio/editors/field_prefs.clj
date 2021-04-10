(ns cf.studio.editors.field-prefs
  (:require [cf.kero.field.tile-layer :as tile-layer]
            [cf.studio.events :as events]
            [cf.studio.i18n :refer [translate-sub]]
            [cljfx.api :as fx]))

(defn- scale-slider
  [{:keys [fx/context text path value-key on-value-changed-event]}]
  {:fx/type :v-box
   :children [{:fx/type :label
               :text (fx/sub-val context translate-sub text)
               :style-class "app-title"}
              {:fx/type :slider
               :min 0.5
               :max 4
               :block-increment 0.5
               :major-tick-unit 1
               :minor-tick-count 1
               :show-tick-labels true
               :show-tick-marks true
               :snap-to-ticks true
               :value (fx/sub-val context get-in [:editors path value-key])
               :on-value-changed {::events/type on-value-changed-event
                                  :path path}}]})

(defn field-prefs-view
  [{:keys [fx/context path]}]
  {:fx/type :grid-pane
   :style-class "app-field-prefs"
   :children (flatten
              [{:fx/type :label
                :grid-pane/row 0
                :grid-pane/column-span 2
                :text (fx/sub-val context translate-sub ::layers)
                :style-class "app-title"}
               (doall
                (for [[layer row] (map vector tile-layer/layers (range 1 4))]
                  [{:fx/type :radio-button
                    :grid-pane/row row
                    :selected (= layer (fx/sub-val context get-in [:editors path :layer]))
                    :on-selected-changed {::events/type ::selected-layer-changed
                                          :path path
                                          :layer layer}}
                   {:fx/type :check-box
                    :grid-pane/row row
                    :grid-pane/column 1
                    :selected (boolean (fx/sub-val context get-in [:editors path :visible-layers layer]))
                    :on-selected-changed {::events/type ::visible-layers-changed
                                          :path path
                                          :layer layer}
                    :style-class ["check-box" "app-text-small"]
                    :text (fx/sub-val context translate-sub (->> layer
                                                                 name
                                                                 (keyword "cf.studio.editors.field-prefs")
                                                                 (fx/sub-val context translate-sub)))}]))
               {:fx/type scale-slider
                :grid-pane/row 4
                :grid-pane/column-span 2
                :text ::layer-scale
                :path path
                :value-key :layer-scale
                :on-value-changed-event ::layer-scale-changed}
               {:fx/type scale-slider
                :grid-pane/row 5
                :grid-pane/column-span 2
                :text ::tileset-scale
                :path path
                :value-key :tileset-scale
                :on-value-changed-event ::tileset-scale-changed}])})

(defmethod events/event-handler ::selected-layer-changed
  [{:keys [fx/context path layer]}]
  {:context (fx/swap-context context assoc-in [:editors path :layer] layer)})

(defmethod events/event-handler ::visible-layers-changed
  [{:keys [fx/context fx/event path layer]}]
  {:context (fx/swap-context context update-in [:editors path :visible-layers] (if event conj disj) layer)})

(defmethod events/event-handler ::layer-scale-changed
  [{:keys [fx/context fx/event path]}]
  {:context (fx/swap-context context assoc-in [:editors path :layer-scale] event)})

(defmethod events/event-handler ::tileset-scale-changed
  [{:keys [fx/context fx/event path]}]
  {:context (fx/swap-context context assoc-in [:editors path :tileset-scale] event)})
