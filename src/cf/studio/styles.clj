(ns cf.studio.styles
  (:require [cljfx.css :as css]))

(def styles
  (css/register
   ::style
   {".app" {"-title" {:-fx-font [:bold 20 :sans-serif]
                      ".label" {:-fx-label-padding 5}}
            "-text" {"-small" {:-fx-font-size 15}
                     "-medium" {:-fx-font-size 20}
                     "-large" {:-fx-font-size 25}}
            "-file-list-cell" {:-fx-font [15 :sans-serif]}}
    ".hbox" {:-fx-spacing 5.0}}))
