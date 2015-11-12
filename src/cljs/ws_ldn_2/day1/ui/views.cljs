(ns ws-ldn-2.day1.ui.views
  (:require-macros
   [reagent.ratom :refer [reaction]]
   [cljs-log.core :refer [debug info warn]])
  (:require
   [ws-ldn-2.day1.ui.state :as state]
   [reagent.core :as r]
   [cljsjs.codemirror :as cm]
   [cljsjs.codemirror.addon.edit.matchbrackets]
   [cljsjs.codemirror.addon.edit.closebrackets]
   [cljsjs.codemirror.mode.clojure :as clj]))

(defn home
  [view]
  [:div.container
   [:h1 "Welcome"]])

(defn cm-editor
  [opts]
  (r/create-class
   {:component-did-mount
    (fn [_]
      (let [editor (.fromTextArea js/CodeMirror (r/dom-node _) (clj->js opts))]
        (.on editor "change" #((:on-change opts) (.getValue %)))))
    :reagent-render
    (fn [_]
      [:textarea {:defaultValue (:defaultValue opts)}])}))

(defn query-editor
  [view]
  (let [query (reaction (:query @state/app-state))]
    [:div.container
     [:div.row
      [:div.col-xs-12
       [:h1 "Query editor"]
       [cm-editor
        {:mode              "text/x-clojure"
         :theme             "material"
         :on-change         #(state/set-query %)
         :defaultValue      @query
         :matchBrackets     true
         :autoCloseBrackets true
         :lineNumbers       true
         :autofocus         true}]]]
     [:div.row
      [:div.col-xs-12
       [:button.btn.btn-primary "Visualize"]]]]))
