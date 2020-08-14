(in-ns 'cf.studio.events.core)

;; TODO Is there any nicer way to go about requiring dependencies without ns?
(require '[cf.kero.field.metadata :as metadata]
         '[cf.kero.field.tile-layer :as tile-layer]
         'clojure.string)

(defmethod event-handler ::pxpack-init-editor
  [{:keys [fx/context] {:keys [path data]} :file}]
  (let [metadata (::pxpack/metadata data)
        spritesheet {(::metadata/spritesheet metadata) ::game-data/spritesheet}
        tilesets (zipmap
                  (map ::metadata/tileset (-> metadata ::metadata/layer-metadata vals))
                  (repeat ::game-data/tileset))]
    {:context (fx/swap-context context update-in [:editors path :visible-layers] #(or % tile-layer/layers))
     :dispatch {::type ::load-dependencies
                :path path
                :dependencies (remove (comp clojure.string/blank? key) (merge spritesheet tilesets))}}))

(defmethod event-handler ::pxpack-visible-tile-layers-changed
  [{:keys [fx/context fx/event path layer]}]
  {:context (fx/swap-context context update-in [:editors path :visible-layers] (if event conj disj) layer)})
