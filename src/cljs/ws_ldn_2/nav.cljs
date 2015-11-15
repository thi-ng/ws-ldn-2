(ns ws-ldn-2.nav
  (:require-macros
   [reagent.ratom :refer [reaction]])
  (:require
   [ws-ldn-2.state :as state]
   [ws-ldn-2.router :as router]))

(defn nav-bar
  [routes route]
  (let [open? (reaction (:nav-open? @state/app-state))]
    [:nav.navbar.navbar-default
     [:div.container-fluid
      [:div.navbar-header
       [:button.navbar-toggle.collapsed
        {:on-click state/nav-toggle-collapse}
        [:span.sr-only "Toggle navigation"]
        [:span.icon-bar]
        [:span.icon-bar]
        [:span.icon-bar]]
       [:span.navbar-brand "WS-LDN-2"]]
      [:div
       {:class (str "collapse navbar-collapse" (if @open? " in"))}
       [:ul.nav.navbar-nav
        (for [r routes :let [uri (router/format-route r {})]]
          [:li {:key (str "nav-" (:id r))}
           [:a {:class    (if (= (:id r) (:id route)) "active")
                :href     uri
                :on-click (router/virtual-link uri)}
            (:label r)]])]]]]))
