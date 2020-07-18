(in-ns 'cf.studio.events.core)

(defmethod event-handler ::switch-pxpack-editor
  [{:keys [path subtype]}]
  {:dispatch {::type ::switch-to-editor
              :editor {:path path :type :cf.kero.field.pxpack/pxpack :subtype subtype}}})
