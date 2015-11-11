(ns ^:figwheel-always ws-ldn-2.day1.ui.core
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [reagent.core :as r]
   [cljs.core.async :as a]))

;; App state handling

(defonce app-state
  (r/atom {}))

;; react components

(defn dropdown
  "Dropdown component. Takes currently selected value, on-change handler
  and a map of menu items, where keys are used as the <option> items' values.
  The map's values are expected to be maps themselves and need to have at
  least a :label key. If the :label is missing the item's key is used as label."
  [sel on-change opts]
  [:select {:defaultValue sel :on-change on-change}
   (map
     (fn [[id val]]
       [:option {:key (str "dd" id) :value (name id)} (or (:label val) (name id))])
     opts)])


(defn main-panel
  "Application main component."
  [id]
  [:div "Hello"])

(defn main
  "Application main entry point, kicks off React component lifecycle."
  []
  (r/render-component [main-panel] (.-body js/document)))

(main)
