(in-ns 'cf.studio.events.core)

;; TODO Is there any nicer way to go about requiring dependencies without ns?
(require '[cf.kero.field.metadata :as metadata]
         'clojure.string)

(defmethod event-handler ::pxpack-create-editor
  [{{:keys [path type] :as file} :file}]
  [[:dispatch {::type ::pxpack-add-dependencies :file file}]
   [:dispatch {::type ::switch-to-editor
  ;; TODO Maintain existing pxpack subeditor selection if editor already exists
               :editor {:path path :type type :subtype ::pxpack/metadata}}]])

(defmethod event-handler ::pxpack-add-dependencies
  [{{:keys [path data]} :file}]
  (let [meta (::pxpack/metadata data)
        spritesheet [(::metadata/spritesheet meta) ::game-data/spritesheet]
        tilesets (map
                  (fn [[_ lm]] [(::metadata/tileset lm) ::game-data/tileset])
                  (::metadata/layer-metadata meta))]
    {:dispatch {::type ::load-dependencies
                :path path
                :dependencies (remove (comp clojure.string/blank? first) (cons spritesheet tilesets))}}))

(defmethod event-handler ::pxpack-switch-editor
  [{:keys [path subtype]}]
  {:dispatch {::type ::switch-to-editor
              :editor {:path path :type ::pxpack/pxpack :subtype subtype}}})
