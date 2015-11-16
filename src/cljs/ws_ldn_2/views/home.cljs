(ns ws-ldn-2.views.home
  (:require-macros
   [reagent.ratom :refer [reaction]]
   [cljs-log.core :refer [debug info warn]]))

(defn ^:export home
  [route]
  [:div.container
   [:h1 "Welcome to our example app"]
   [:div.row
    [:div.col-xs-12
     [:p "Not much to see on this page, but do try out the other sections..."]
     [:p "For more information visit: "
      [:a {:href "https://github.com/thi-ng/ws-ldn-2/"} "the workshop repo on Github"] " & the "
      [:a {:href "http://thi.ng"} "thi.ng"] " website"]]]])
