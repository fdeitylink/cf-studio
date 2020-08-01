(in-ns 'cf.studio.events.core)

;; TODO Is there any nicer way to go about requiring dependencies without ns?
(require '[cf.kero.field.metadata :as metadata]
         'clojure.string)

(defmethod event-handler ::pxpack-add-dependencies
  [{{:keys [path data]} :file}]
  (let [metadata (::pxpack/metadata data)
        spritesheet {(::metadata/spritesheet metadata) ::game-data/spritesheet}
        tilesets (zipmap
                  (map ::metadata/tileset (-> metadata ::metadata/layer-metadata vals))
                  (repeat ::game-data/tileset))]
    {:dispatch {::type ::load-dependencies
                :path path
                :dependencies (remove (comp clojure.string/blank? key) (merge spritesheet tilesets))}}))
