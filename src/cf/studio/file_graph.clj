(ns cf.studio.file-graph
  "Namespace for managing Cat & Frog Studio's file graph.
  Manages opening, closing, editing files, and their dependencies on other files."
  (:require [cljfx.api :as fx]
            loom.attr
            loom.graph))

;; TODO More cljfx context integration? Reevaluate, given new features in cljfx 1.7.5 (fx/sub-ctx & fx/sub-val)
;; Seems like a bad pattern to use fx/swap-context here (is it?), so we have events manage the swapping
;;  - e.g. (fx/swap-context context assoc-file-data path data)
;;  - Using fx/swap-context here would put intermediary (often "invalid") states into the context cache
;;    - Having the "root" function use contexts and the "child" functions not would solve this,
;;      but then we're mixing between contexts and not-contexts, and also some functions that are
;;      "children" in some cases are "roots" in others. Not to mention this drops the benefit of fx/sub-ctx.
;; The issue is that fx/sub-ctx fns take the 'full' context map (i.e. containing cljfx internals)
;; while fx/swap-context fns take just the state map (i.e. no internals, just app state)
;; so fns using swap-context couldn't rely on fx/sub-ctx fns, which makes integration feel pointless.
;; sub-val would work here, but it doesn't use caching since it's meant for quick ops (e.g. get).
;;
;; Integration might also add unworthwhile complexity and coupling to this ns, especially when some
;; fns invoke other context-based fns in this ns *and* loom fns (not context-based, just take the graph)
;;
;; Benefit would be that using fx/sub-ctx takes advantage of cljfx cache

(defn create-file
  "Adds `file` to `files`."
  [files {:keys [path] :as file}]
  (reduce
   (fn [files [k v]] (loom.attr/add-attr files path k v))
   (loom.graph/add-nodes files path)
   (merge {:open? false :editing? false} file)))

;; TODO The dependencies for this are far more complicated...
#_(defn remove-file
    [files path])

(defn file-graph
  "Creates a new file graph from `files`."
  [& files]
  (reduce create-file (loom.graph/digraph) files))

(defn file
  "Returns the file map corresponding to `path`."
  [files path]
  (loom.attr/attrs files path))

(defn paths
  "Returns a set of the paths in `files`."
  [files]
  (loom.graph/nodes files))

(defn files
  "Returns a set of the files in `fyles`."
  [fyles]
  (set (map (partial file fyles) (paths fyles))))

(defn file-type
  "Returns the type of the file corresponding to `path`."
  [files path]
  (loom.attr/attr files path :type))

(defn file-data
  "Returns the data of the file corresponding to `path`."
  [files path]
  (loom.attr/attr files path :data))

(defn assoc-file-data
  "Assoc's new data to the file corresponding to `path`."
  [files path data]
  (loom.attr/add-attr files path :data data))

(defn update-file-data
  "Updates the data of the file corresponding to `path`.
  `f` is a function taking the existing data and producing new data."
  [files path f]
  (assoc-file-data files path (f (file-data files path))))

(defn file-dependencies
  "Returns a set of paths that `path` depends on."
  [files path]
  (loom.graph/successors files path))

(defn- create-edges
  "Creates a sequence of vector edges between `path` and each item in `dependencies`."
  [path dependencies]
  (map (partial vector path) dependencies))

;; TODO Ensure editing? first?
(defn add-file-dependencies
  "Adds `dependencies` as dependencies to `path`."
  [files path dependencies]
  (loom.graph/add-edges* files (create-edges path dependencies)))

(defn remove-file-dependencies
  "Removes `dependencies` as dependencies to `path`."
  [files path dependencies]
  (loom.graph/remove-edges* files (create-edges path dependencies)))

(defn swap-file-dependency
  "Replaces `path`'s `old` dependency with `new`."
  [files path old new]
  (-> files
      (remove-file-dependencies path [old])
      (add-file-dependencies path [new])))

(defn is-file-open?
  "Checks if the file corresponding to `path` is open."
  [files path]
  (loom.attr/attr files path :open?))

(defn is-file-closed?
  "Checks if the file corresponding to `path` is closed."
  [files path]
  (not (is-file-open? files path)))

(defn editing-file?
  "Checks if the file corresponding to `path` is being edited."
  [files path]
  (loom.attr/attr files path :editing?))

(defn open-file
  "Marks the file corresponding to `path` as open and assoc's in `data`."
  [files path data]
  (-> files
      (loom.attr/add-attr path :open? true)
      (assoc-file-data path data)))

(defn- can-close-file?
  "Checks if the file corresponding to `path` can be closed.
  Files are closeable if they are not being edited and are not
  registered as dependencies for other files."
  [files path]
  (and
   (not (editing-file? files path))
   (zero? (loom.graph/in-degree files path))))

(defn- close-file
  "If the file corresponding to `path` is open and is closeable, marks it as
  closed and removes its data."
  [files path]
  (if (and (is-file-open? files path) (can-close-file? files path))
    (-> files
        (loom.attr/add-attr path :open? false)
        (loom.attr/remove-attr path :data))
    files))

;; TODO Ensure open? first?
(defn open-editor
  "Marks a file correspondding to `path` as open for editing."
  [files path]
  (loom.attr/add-attr files path :editing? true))

(defn close-editor
  "If the file corresponding to `path` is being edited, closes its editor, removes
  its dependencies, and closes the file and its dependencies where possible."
  [files path]
  (when (editing-file? files path)
    (let [deps (file-dependencies files path)]
      (as-> files files
        (loom.attr/add-attr files path :editing? false)
        (remove-file-dependencies files path deps)
        (reduce close-file files (cons path deps))))))

(defn- filter-nodes
  "Same as `loom.derived/nodes-filtered-by` but retains attributes."
  [pred g]
  (loom.graph/remove-nodes* g (remove pred (loom.graph/nodes g))))

(defn filter-file-attr
  "Filters `files` for files where the attribute `k` has value `v`."
  [files k v]
  (filter-nodes #(= v (loom.attr/attr files % k)) files))

(defn filter-file-type
  "Filters `files` for files with type `type`."
  [files type]
  (filter-file-attr files :type type))

(defn filter-files
  "Filters `files` according to `pred`.
  `pred` is a predicate function taking `files` and a node as arguments."
  [files pred]
  (filter-nodes (partial pred files) files))

(defn filter-open-files
  "Filters `files` for open files."
  [files]
  (filter-files files is-file-open?))

(defn filter-editing-files
  "Filters `files` for files being edited."
  [files]
  (filter-files files editing-file?))

(defn filter-closed-files
  "Filters `files` for closed files."
  [files]
  (filter-files files is-file-closed?))

(defmacro ^:private create-sub-fns
  "Turns functions of file graphs into functions of cljfx contexts.
  `fn-names` is a list of function symbols. Each new function will
  have `-sub` appended to its name and take the same arguments, except
  with a context as the first argument instead of a file graph."
  [& fn-names]
  `(do
     ~@(for [fn-name fn-names]
         ;; Getting the real arglist makes the docs cleaner than using [context# & rest#]
         (let [argvec (-> fn-name resolve meta :arglists first (assoc 0 'context))]
           `(defn ~(-> fn-name (str "-sub") symbol)
              ~(str "`fx/sub-ctx` version of `" fn-name "`.")
              ~argvec
              (~fn-name (fx/sub-val ~(first argvec) :files) ~@(rest argvec)))))))

(create-sub-fns
 file
 paths
 files
 file-type
 file-data
 file-dependencies
 is-file-open?
 is-file-closed?
 editing-file?
 filter-file-attr
 filter-file-type
 filter-files
 filter-open-files
 filter-closed-files
 filter-editing-files)
