(ns cf.kero.game-data
  (:require [cf.kero.field.pxpack :as pxpack]
            [cf.util :as util]
            clojure.set
            [clojure.spec.alpha :as spec]
            clojure.string
            [me.raynes.fs :as fs])
  (:import javafx.scene.image.Image))

(def resource-type->path-meta
  "Map of resource type keywords to metadata for their paths."
  ;; Aliasing for nonexistent namespaces: https://clojure.atlassian.net/browse/CLJ-2123
  {::music {:subdir "bgm" :prefix "" :extension ".ptcop"}
   ;; TODO Consider cf.kero.field prefix
   ;; Maybe when an alias like ::field is possible
   ::pxpack/pxpack {:subdir "field" :prefix "" :extension ".pxpack"}
   ::image {:subdir "img" :prefix "" :extension ".png"}
   ::spritesheet {:subdir "img" :prefix "fu" :extension ".png"}
   ::tileset {:subdir "img" :prefix "mpt" :extension ".png"}
   ::tile-attribute {:subdir "img" :prefix "" :extension ".pxattr"}
   ::sfx {:subdir "se" :prefix "" :extension ".ptnoise"}
   ::script {:subdir "text" :prefix "" :extension ".pxeve"}})

(defn- image
  "Constructs an `Image` from `path`."
  [path]
  (->> path
       (str "file:///")
       Image.))

(def resource-type->reader-fn
  "Map of resource type keywords to file codecs"
  {::pxpack/pxpack (partial util/decode-file pxpack/pxpack-codec)
   ::image image
   ::spritesheet image
   ::tileset image})

(def resource-dir->game-type
  "Map of resource directory names to Kero Blaster base game types."
  {"rsc_k" ::kero-blaster "rsc_p" ::pink-hour})

;; TODO Consider returning a set of {:path: _ :type _}
(defn- resource-files
  "Returns a set of resource files.
  `resource-dir` is the root resource directory.
  `subdir` is the specific subdirectory in `resource-dir` for the desired resource type.
  `extension` is the filename extension of the resource type.
  `prefix` is the optional filename prefix of the resource type."
  [resource-dir {:keys [subdir prefix extension]}]
  (set (fs/find-files
        (fs/file resource-dir subdir)
        (re-pattern (str "^" prefix ".+\\" extension "$")))))

(spec/def ::executable (spec/and #(= (fs/extension %) ".exe") fs/file?))
(spec/def ::resource-dir (spec/and (comp #{"rsc_k" "rsc_p"} fs/base-name) fs/directory?))

(defmacro ^:private def-resource-type-specs
  "Defines the specs for the resource types"
  []
  `(do
     ~@(for [[resource-type {:keys [prefix extension]}] resource-type->path-meta]
         `(spec/def ~resource-type
            (spec/coll-of
             ;; Every resource path must have the correct parent directory, prefix, and extension
             (spec/and
              ;; TODO Find way to do resource subdir check from this spec and not ::metdatada spec
              #(clojure.string/starts-with? (fs/base-name %) ~prefix)
              #(= (fs/extension %) ~extension))
             :kind set?
             :distinct true)))))

(def-resource-type-specs)

(spec/def ::game-data (spec/and
                      (spec/keys :req (conj (keys resource-type->path-meta) ::executable ::resource-dir))
                      ;; Validate parent directories of paths in resource path sets
                      (fn [{:keys [::resource-dir] :as game-data}]
                        ;; For every set
                        (every?
                         (fn [[resource-type resource-path-set]]
                           (let [{:keys [subdir]} (resource-type resource-type->path-meta)
                                 parent (fs/file resource-dir subdir)]
                             ;; For every path in the set
                             (every? #(= (fs/parent %) parent) resource-path-set)))
                         (dissoc game-data ::executable ::resource-dir)))))

;; TODO Store localize file paths
(defn executable->game-data
  "Creates a game data map from a Kero Blaster executable path.
  `executable-path` is the path of the Kero Blaster executable.
  Keys are namespaced under this namespace, unless otherwise specified, and will map to:
   - `:executable` - the path to the game executable
   - `:resource-dir` - the path to the game's root resource directory
   - `:music` - a set of the game's background music file paths
   - `::pxpack/pxpack` - a set of the game's field file paths
   - `:image` - a set of the game's image file paths besides the spritesheets and tilesets
   - `:spritesheet` - a set of the game's spritesheet file paths
   - `:tileset` - a set of the game's tileset file paths
   - `:tile-attribute` - a set of the game's tile attribute file paths
   - `:sfx` - a set of the game's sound effects file paths
   - `:script` - a set of the game's script file paths"
  [executable-path]
  (let [resource-dir (first (fs/find-files (fs/parent executable-path) #"rsc_[kp]"))]
    (->> resource-type->path-meta
         ;; Get all the path resources
         (map (fn [[resource-kw path-meta]] [resource-kw (resource-files resource-dir path-meta)]))
         ;; Add executable path and root resource directory path to map
         (into {::executable (fs/absolute executable-path) ::resource-dir resource-dir})
         ;; Fix ::images - should not contain elements in ::tilesets or ::spritesheets
         (#(assoc % ::images (clojure.set/difference (::images %) (::tilesets %) (::spritesheets %)))))))
