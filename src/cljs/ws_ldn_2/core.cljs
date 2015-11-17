(ns ^:figwheel-always ws-ldn-2.core
  (:require-macros
   [reagent.ratom :refer [reaction]]
   [cljs.core.async.macros :refer [go go-loop]]
   [cljs-log.core :refer [debug info warn]])
  (:require
   [ws-ldn-2.state :as state]
   [ws-ldn-2.router :as router]
   [ws-ldn-2.nav :as nav]
   [ws-ldn-2.views.home :as viewshome]
   [ws-ldn-2.views.map :as viewsmap]
   [ws-ldn-2.views.query :as viewsquery]
   [ws-ldn-2.utils :as utils]
   [reagent.core :as r]
   [cljs.core.async :as a]
   [thi.ng.validate.core :as v]))

(def routes
  "Basic SPA route configuration. See router ns for further options."
  [{:id        :home
    :match     ["home"]
    :component #'viewshome/home
    :label     "Home"}
   {:id        :housing-map
    :match     ["housing"]
    :component #'viewsmap/heatmap-view
    :label     "House prices"}
   {:id        :query-edit
    :match     ["query"]
    :component #'viewsquery/query-editor
    :label     "Query editor"}])

(defn view-wrapper
  "Shared component wrapper for all routes, includes navbar."
  [route]
  (let [route @route]
    [:div
     [nav/nav-bar routes route]
     [(:component route) route]]))

(defn app-component
  "Application main component."
  []
  (let [route      (reaction (:curr-route @state/app-state))
        ready?     (reaction (:app-ready? @state/app-state))
        init-state (reaction (:init-state @state/app-state))]
    (fn []
      (if (and @ready? @route)
        [view-wrapper route]
        (let [{:keys [id progress total]} @init-state]
          [:div [:h1 (state/init-states id) (when progress (str " " progress "/" total))]])))))

(defn start-router
  "Starts SPA router, called from main fn."
  []
  (router/start!
   routes
   nil
   (router/route-for-id routes :home)
   state/nav-change
   (constantly nil)))

(defn main
  "Application main entry point, initializes app state and kicks off
  React component lifecycle."
  []
  (when-not (:inited @state/app-state)
    (state/init-app))
  (start-router)
  (r/render-component [app-component] (.-body js/document)))

(defn on-js-reload
  "Called each time fighweel has reloaded code."
  [] (debug :reloaded))

(main)
