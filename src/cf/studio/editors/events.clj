(ns cf.studio.editors.events
  (:require [cf.studio.events :as events]))

(defmethod events/event-handler ::events/switch-pxpack-editor
  [{:keys [path type]}]
  {:dispatch {::events/type ::events/switch-to-editor :editor-path [path type]}})
