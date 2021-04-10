(ns cf.studio.events
  (:require [cf.kero.field.metadata :as metadata]
            [cf.kero.field.pxpack :as pxpack]
            [cf.kero.field.tile-layer :as tile-layer]
            [cf.kero.game-data :as game-data]
            [cf.studio.effects :as effects]
            [cf.studio.file-graph :as file-graph]
            [cljfx.api :as fx]
            clojure.string
            [me.raynes.fs :as fs]))

(defmulti event-handler ::type)

;; These events simply delegate to effects
;; Used when a function or property expects an event but an effect is needed

(defmethod event-handler ::exception
  [event-map]
  {::effects/exception-dialog event-map})

(defmethod event-handler ::shutdown
  [event-map]
  {::effects/shutdown event-map})

;; These events relate to reading files

(defmethod event-handler ::open-file
  [{:keys [fx/context path edit?]}]
  (let [file (fx/sub-ctx context file-graph/file-sub path)]
    (if (fx/sub-ctx context file-graph/is-file-open?-sub path)
      (when edit? {:dispatch {::type ::init-editor :file file}})
      {::effects/read-file {:file file
                            :reader-fn (game-data/resource-type->reader-fn (:type file))
                            :on-complete {::type ::load-file :edit? edit?}
                            :on-exception {::type ::exception}}})))

(defmethod event-handler ::load-file
  [{:keys [fx/context edit?] {:keys [path data] :as file} :file}]
  (merge
   {:context (fx/swap-context context update :files file-graph/open-file path data)}
   (when edit? {:dispatch {::type ::init-editor :file file}})))

;; These events relate to managing editors

(defmethod event-handler ::init-editor
  [{:keys [fx/context] {:keys [path type] :as file} :file}]
  (concat
   [[:context (fx/swap-context context update :files file-graph/open-editor path)]
    [:dispatch {::type (case type
                         ::pxpack/pxpack ::pxpack-init-editor)
                :file file}]
    [:dispatch {::type ::switch-editor :path path}]]))

(defmethod event-handler ::pxpack-init-editor
  [{:keys [fx/context] {:keys [path data]} :file}]
  (let [metadata (::pxpack/metadata data)
        spritesheet {(::metadata/spritesheet metadata) ::game-data/spritesheet}
        tilesets (zipmap
                  (->> metadata ::metadata/layer-metadata vals (map ::metadata/tileset))
                  (repeat ::game-data/tileset))]
    {:context (fx/swap-context context update-in [:editors path] (partial merge
                                                                          {:layer (first tile-layer/layers)
                                                                           :visible-layers tile-layer/layers
                                                                           :layer-scale 2
                                                                           :tileset-scale 2}))
     :dispatch {::type ::load-dependencies
                :path path
                :dependencies (remove (comp clojure.string/blank? key) (merge spritesheet tilesets))}}))

(defmethod event-handler ::load-dependencies
  [{:keys [fx/context path dependencies]}]
  (let [dep-paths (map
                   ;; TODO Handle localize items
                   (fn [[dep-name type]]
                     (let [resource-dir (fx/sub-val context get-in [:game-data ::game-data/resource-dir])
                           {:keys [subdir extension]} (type game-data/resource-type->path-meta)]
                       (fs/file resource-dir subdir (str dep-name extension))))
                   dependencies)]
    (cons
     [:context (fx/swap-context context update :files file-graph/add-file-dependencies path dep-paths)]
     (for [dep-path dep-paths]
       [:dispatch {::type ::open-file :path dep-path :edit? false}]))))

(defmethod event-handler ::switch-editor
  [{:keys [fx/context path]}]
  {:context (fx/swap-context context assoc :editor path)})

;; TODO Use prompt effect (see todo on ::close-mod)
(defmethod event-handler ::close-editor
  [{:keys [fx/context path]}]
  {:context (fx/swap-context context
                             #(as-> % ctxt
                                (update ctxt :files file-graph/close-editor path)
                                (if (= path (:editor ctxt))
                                  (dissoc ctxt :editor)
                                  ctxt)))})
