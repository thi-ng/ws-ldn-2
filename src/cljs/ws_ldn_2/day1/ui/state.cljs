(ns ws-ldn-2.day1.ui.state
  (:require-macros
   [reagent.ratom :refer [reaction]]
   [cljs-log.core :refer [debug info warn]])
  (:require
   [reagent.core :as r]))

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
