(ns kero-edit.kero.gamedata
  (:require [clojure.set]
            [clojure.string]
            [clojure.spec.alpha :as spec]
            [me.raynes.fs :as fs]))

(def resource-path-metadata
  "Map of resource type keywords to metadata for their paths."
  {::music {:resource-subdir "bgm" :prefix "" :extension ".ptcop"}
   ::fields {:resource-subdir "field" :prefix "" :extension ".pxpack"}
   ::images {:resource-subdir "img" :prefix "" :extension ".png"}
   ::spritesheets {:resource-subdir "img" :prefix "fu" :extension ".png"}
   ::tilesets {:resource-subdir "img" :prefix "mpt" :extension ".png"}
   ::tile-attributes {:resource-subdir "img" :prefix "" :extension ".pxattr"}
   ::sfx {:resource-subdir "se" :prefix "" :extension ".ptnoise"}
   ::scripts {:resource-subdir "text" :prefix "" :extension ".pxeve"}})

(def resource-dir->game-type
  "Map of resource directory names to Kero Blaster base game types."
  {"rsc_k" ::kero-blaster "rsc_p" ::pink-hour})

(defn- resource-files
  "Returns an alphabetically sorted set of resource files.
  `resource-dir` is the root resource directory.
  `resource-subdir` is the specific subdirectory in `resource-dir` for the desired resource type.
  `extension` is the filename extension of the resource type.
  `prefix` is the optional filename prefix of the resource type."
  ([resource-dir {:keys [resource-subdir prefix extension]}]
   (apply sorted-set (fs/find-files
                      (fs/file resource-dir resource-subdir)
                      (re-pattern (str "^" prefix ".+\\" extension + "$"))))))

(spec/def ::executable (spec/and #(= (fs/extension %) ".exe") fs/file?))
(spec/def ::resource-dir (comp #{"rsc_k" "rsc_p"} fs/base-name))
(spec/def ::gamedata (spec/and
                      (spec/keys :req (conj (keys resource-path-metadata) ::executable ::resource-dir))
                      ;; Validate the resource path sets
                      (fn [{:keys [::resource-dir] :as gd}]
                        (every?
                         (fn [[resource-kw resource-path-set]]
                           (let [{:keys [resource-subdir prefix extension]} (resource-kw resource-path-metadata)]
                             (and
                              ;; Every resource path collection must be a sorted set
                              ;; TODO Consider relaxing this requirement
                              (set? resource-path-set)
                              (sorted? resource-path-set)
                              ;; Every resource path must have the correct parent directory, prefix, and extension
                              (every?
                               #(and
                                 (= (fs/parent %) (fs/file resource-dir resource-subdir))
                                 (clojure.string/starts-with? (fs/base-name %) prefix)
                                 (= (fs/extension %) extension))
                               resource-path-set))))
                         (seq (dissoc gd ::executable ::resource-dir))))))

;; TODO Store localize file paths
(defn executable->gamedata
  "Creates a gamedata map from a Kero Blaster executable path.
  `executable-path` is the path of the Kero Blaster executable.
  The map is namespaced under `kero-edit.kero.gamedata` and will contain:
   - `:executable` - the path to the game executable
   - `:resource-dir` - the path to the game's root resource directory
   - `:music` - a sorted set of the game's background music file paths
   - `:fields` - a sorted set of the game's field file paths
   - `:images` - a sorted set of the game's image file paths besides the spritesheets and tilesets
   - `:spritesheets` - a sorted set of the game's spritesheet file paths
   - `:tilesets` - a sorted set of the game's tileset file paths
   - `:tile-attributes` - a sorted set of the game's tile attribute file paths
   - `:sfx` - a sorted set of the game's sound effects file paths
   - `:scripts` - a sorted set of the game's script file paths"
  [executable-path]
  (let [resource-dir (first (fs/find-files (fs/parent executable-path) (re-pattern "rsc_[kp]")))]
    (->> resource-path-metadata
         ;; Get all the path resources
         (map (fn [[resource-kw path-meta]] [resource-kw (resource-files resource-dir path-meta)]))
         ;; Add executable path and root resource directory path to map
         (into {::executable (fs/absolute executable-path) ::resource-dir resource-dir})
         ;; Fix ::images - should not contain elements in ::tilesets or ::spritesheets
         (#(assoc % ::images (clojure.set/difference (::images %) (::tilesets %) (::spritesheets %)))))))
