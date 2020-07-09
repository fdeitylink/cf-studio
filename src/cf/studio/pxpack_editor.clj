(ns cf.studio.pxpack-editor)

(defn pxpack-editor
  [{:keys [fx/context editor]}]
  {:fx/type :stack-pane
   :children [{:fx/type :text
               :stack-pane/alignment :center
               :stack-pane/margin 10
               :text (-> editor :path str)
               :font {:family "" :size 15}}]})
