(ns ws-ldn-2.day1.ui.state
  (:require-macros
   [reagent.ratom :refer [reaction]]
   [cljs-log.core :refer [debug info warn]])
  (:require
   [reagent.core :as r]
   [thi.ng.domus.io :as io]))

(defonce app-state (r/atom {}))

(defn set-state!
  [key val]
  (info key val)
  (swap! app-state (if (sequential? key) assoc-in assoc) key val))

(defn update-state!
  [key f & args]
  (info key f args)
  (swap! app-state #(apply (if (sequential? key) update-in update) % key f args)))

(defn nav-change
  [route] (set-state! :curr-route route))

(defn nav-toggle-collapse
  [] (update-state! :nav-collapsed? not))

(defn set-query
  [q] (set-state! :query q))

(defn submit-query
  []
  (io/request
   {:uri     "http://localhost:8000/query"
    :method  :post
    :data  {:spec (:query @app-state)}
    :success (fn [status data] (info :response data))
    :error   (fn [status msg] (warn :error status msg))}))

(defn set-viz-query
  []
  (set-state! :query-viz-uri
              (str "/queryviz?"
                   (io/->request-data
                    {:spec (:query @app-state)
                     :format "png"}))))

(defn init-map
  [] (debug :init-map))
