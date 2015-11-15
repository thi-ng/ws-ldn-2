(ns ws-ldn-2.components.editor
  (:require
   [reagent.core :as r]
   [cljsjs.codemirror :as cm]
   [cljsjs.codemirror.addon.edit.matchbrackets]
   [cljsjs.codemirror.addon.edit.closebrackets]
   [cljsjs.codemirror.addon.selection.active-line]
   [cljsjs.codemirror.mode.clojure]))

(defn cm-editor
  [props cm-opts]
  (r/create-class
   {:component-did-mount
    (fn [this]
      (let [editor (.fromTextArea js/CodeMirror (r/dom-node this) (clj->js cm-opts))]
        (.on editor "change" #((:on-change props) (.getValue %)))
        (r/set-state this {:editor editor})))

    :should-component-update
    (fn [this]
      (let [editor  (:editor (r/state this))
            val     @(:state props)
            update? (not= val (.getValue editor))]
        (when update? (.setValue editor val))
        update?))
    
    :reagent-render
    (fn [_] [:textarea {:default-value (:default-value props)}])}))
