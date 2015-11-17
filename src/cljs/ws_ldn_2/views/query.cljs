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

(defn result-row
  [i row qvars id-col?]
  (let [k (str "result" i)]
    [:tr {:key k}
     (if id-col? [:td {:key (str k "__id")} i])
     (map (fn [q] [:td {:key (str k "-" q)} (pr-str (row q))]) qvars)]))

(defn query-results-table
  "User query results table component."
  [table]
  (when table
    (let [{:keys [body qvars grouped]} table]
      [:div.row
       [:div.col-xs-12
        [:h3 "Query results"]
        [:table.table.table-condensed.query-results
         [:tbody
          [:tr
           (map-indexed
            (fn [i qvar] [:th {:key (str "qvar-" qvar)} (pr-str qvar)])
            (cons 'id qvars))]
          (map-indexed
           (fn [i r] (result-row i r qvars true))
           body)]]]])))

(defn query-viz
  "Query viz component displaying structure of query in editor.
  Only active when :query-viz-uri is set in app-state."
  []
  (let [src (state/subscribe :query-viz-uri)]
    (fn []
      (when @src
        [:div.row
         [:div.col-xs-12
          [:h2 "Query structure"]
          [:img.fullwidth {:src @src}]]]))))

(defn ^:export query-editor
  "Main component for query view/route"
  [route]
  (let [query    (state/subscribe :query)
        q-preset (state/subscribe :query-preset)
        table    (state/subscribe :user-query-results)]
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
          {:on-click state/submit-user-query}
          "Submit"] "\u00a0"
         [:button.btn.btn
          {:on-click state/set-viz-query}
          "Visualize"]]
        [:div.col-xs-6
         [dd/dropdown
          "dd-query-preset"
          @q-preset
          #(state/set-query-preset (utils/event-value-id %))
          state/query-presets]]]
       [query-results-table @table]
       [query-viz]])))
