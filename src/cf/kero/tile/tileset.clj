(ns cf.kero.tile.tileset)

(def tile-width
  "The width, in pixels, of a tile."
  8)

(def tile-height
  "The height, in pixels, of a tile."
  8)

(def tiles-per-row
  "The number of tiles per row in a tileset."
  16)

(def tiles-per-column
  "The number of tiles per column in a tileset."
  16)

(def tileset-width
  "The width, in pixels, of a tileset."
  (* tile-width tiles-per-row))

(def tileset-height
  "The height, in pixels, of a tileset."
  (* tile-height tiles-per-column))

(defn tile->xy
  "Converts `tile` to a vector of x and y coordinates.
  `width` defaults to `tiles-per-row`."
  ([tile] (tile->xy tile tiles-per-row))
  ([tile width] [(mod tile width) (int (/ tile width))]))

(defn xy->tile
  "Converts `x` and `y` to a tile.
  `width` defaults to `tiles-per-row`."
  ([x y] (xy->tile x y tiles-per-row))
  ([x y width] (+ (* y width) x)))
