(ns cf.studio.editors.pxpack.events
  (:require [cf.studio.events :as events]))

(defmethod events/event-handler ::events/switch-pxpack-editor
  [{:keys [path subtype]}]
  {:dispatch {::events/type ::events/switch-to-editor
              :editor {:path path :type :cf.kero.field.pxpack/pxpack :subtype subtype}}})
