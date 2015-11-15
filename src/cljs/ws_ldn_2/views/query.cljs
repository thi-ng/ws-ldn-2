(ns ws-ldn-2.views.query
  (:require-macros
   [reagent.ratom :refer [reaction]]
   [cljs-log.core :refer [debug info warn]])
  (:require
   [ws-ldn-2.state :as state]
   [ws-ldn-2.utils :as utils]
   [ws-ldn-2.components.dropdown :as dd]
   [ws-ldn-2.components.editor :as editor]
   [reagent.core :as r]))

(defn query-viz
  []
  (let [src (state/subscribe :query-viz-uri)]
    (fn []
      (when @src
        [:div.row
         [:div.col-xs-12
          [:h2 "Query structure"]
          [:img.fullwidth {:src @src}]]]))))

(defn ^:export query-editor
  [route]
  (let [query    (state/subscribe :query)
        q-preset (state/subscribe :query-preset)]
    (fn [route]
      [:div.container
       [:div.row
        [:div.col-xs-12
         [:h1 "Query editor"]
         [editor/cm-editor
          {:on-change     state/set-query
           :default-value @query
           :state         query}
          {:mode              "text/x-clojure"
           :theme             "solarized light"
           :matchBrackets     true
           :autoCloseBrackets true
           :styleActiveLine   true
           :lineNumbers       true
           :autofocus         true}]]]
       [:div.row
        [:div.col-xs-6
         [:button.btn.btn-primary
          {:on-click #(state/submit-oneoff-query @query nil)} ;; TODO chan
          "Submit"] "\u00a0"
         [:button.btn.btn
          {:on-click state/set-viz-query}
          "Visualize"]]
        [:div.col-xs-6
         [dd/dropdown
          @q-preset
          #(state/set-query-preset (utils/event-value-id %))
          state/query-presets]]]
       [query-viz]])))
