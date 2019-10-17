(ns kero-edit.edit.settings-grid
  (:require [cljfx.api :as fx]))

(defn settings-grid
  [{:keys [fx/context]}]
  {:fx/type :grid-pane
   :padding 10
   :vgap 10
   :hgap 20})
