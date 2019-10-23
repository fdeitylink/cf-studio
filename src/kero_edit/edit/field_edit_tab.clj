(ns kero-edit.edit.field-edit-tab
  (:require [me.raynes.fs :as fs]
            [cljfx.api :as fx]
            [kero-edit.edit.events :as events]))

(defn field-edit-tab
  [{:keys [fx/context field]}]
  (let [name (fs/base-name (:path field) true)]
    {:fx/type :tab
     :id name
     :text name
     :on-closed {::events/type ::events/close-field :field field}}))
