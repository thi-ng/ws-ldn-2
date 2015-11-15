(ns ws-ldn-2.views.home
  (:require-macros
   [reagent.ratom :refer [reaction]]
   [cljs-log.core :refer [debug info warn]]))

(defn ^:export home
  [route]
  [:div.container
   [:h1 "Welcome"]])
