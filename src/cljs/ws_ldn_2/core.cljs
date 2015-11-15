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
  [route]
  (let [route @route]
    [:div
     [nav/nav-bar routes route]
     [(:component route) route]]))

(defn main-panel
  "Application main component."
  []
  (let [route (reaction (:curr-route @state/app-state))]
    (fn []
      (if @route
        [view-wrapper route]
        [:div "Initializing..."]))))

(defn start-router
  []
  (router/start!
   routes
   nil
   (router/route-for-id routes :home)
   state/nav-change
   (constantly nil)))

(defn main
  "Application main entry point, kicks off React component lifecycle."
  []
  (when-not (:inited @state/app-state)
    (state/init-app))
  (start-router)
  (r/render-component [main-panel] (.-body js/document)))

(defn on-js-reload
  [] (debug :reloaded))

(main)
