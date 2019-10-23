(ns kero-edit.edit.field-edit-tab
  (:require [me.raynes.fs :as fs]
            [cljfx.api :as fx]
            [kero-edit.kero.field.pxpack :as pxpack]
            [kero-edit.kero.field.head :as head]
            [kero-edit.edit.events :as events]))

(defn field-edit-tab
  [{:keys [fx/context field]}]
  (let [name (fs/base-name (:path field) true)]
    {:fx/type :tab
     :id name
     :text name
     :tooltip {:fx/type :tooltip
               :text (get-in field [:data ::pxpack/head ::head/description])}
     :on-closed {::events/type ::events/close-field :field field}}))
