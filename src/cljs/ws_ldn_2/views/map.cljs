(ns ws-ldn-2.views.map
  (:require-macros
   [reagent.ratom :refer [reaction]]
   [cljs-log.core :refer [debug info warn]])
  (:require
   [ws-ldn-2.state :as state]
   [ws-ldn-2.components.dropdown :as dd]
   [ws-ldn-2.utils :as utils]
   [reagent.core :as r]
   [thi.ng.geom.core :as g]
   [thi.ng.geom.core.matrix :as mat]
   [thi.ng.geom.svg.core :as svg]
   [thi.ng.color.core :as col]
   [thi.ng.color.gradients :as grad]
   [thi.ng.math.core :as m]
   [thi.ng.strf.core :as f]))

(def heatmaps
  "Preset configs of gradient IDs and their direction"
  (sorted-map
   :green-cyan          {:flipped false}
   :blue-cyan           {:flipped false}
   :green-magenta       {:flipped false}
   :orange-blue         {:flipped true}
   :rainbow1            {:flipped true}
   :yellow-magenta-cyan {:flipped true}))

(defn heatmap-color
  "Higher order fn. Takes min/max vals of domain range and gradient preset ID.
  Returns single-arg fn used to compute heatmap color (as CSS string)."
  [min max gradient-id]
  (let [[offset amp fmod phase] (grad/cosine-schemes gradient-id)
        flipped? (-> heatmaps gradient-id :flipped)]
    (fn [x]
      (let [t (m/clamp (m/map-interval x min max 0.0 1.0) 0.0 1.0)
            t (if flipped? (- 1.0 t) t)]
        (->> t
             (grad/cosine-gradient-color offset amp fmod phase)
             (col/as-css)
             (deref))))))

(defn map-overlay
  "Heatmap tooltip component, displaying details of selected borough."
  [data selected]
  (when selected
    (let [{:keys [centroid name id avg min max num]} (data selected)
          centroid (g/min (g/max centroid [120 100]) [840 720])
          ff utils/format-gbp]
      (svg/group
       {:key "overlay" :transform (g/translate mat/M32 centroid) :stroke "none"}
       (svg/polygon
        [[-120 -100] [120 -100] [120 0] [10 0] [0 10] [-10 0] [-120 0]]
        {:key "overlay-bg" :fill (col/rgba [0 0 0 0.8])})
       (svg/group
        {:key "overlay-body" :fill "#fff" :style {:font-size "12px"}}
        (svg/text
         [-110 -75] name {:key (str id "-name") :style {:font-size "18px"}})
        (svg/text
         [-110 -50] (f/format ["Avg: £" ff] avg) {:key (str id "-avg")})
        (svg/text
         [-110 -30] (f/format ["Min: £" ff] min) {:key (str id "-min")})
        (svg/text
         [-110 -10] (f/format ["Max: £" ff] max) {:key (str id "-max")})
        (svg/line
         [20 -60] [20 -10] {:stroke "#fff"})
        (svg/text
         [70 -37] num {:text-anchor "middle" :style {:font-size "24px"}})
        (svg/text
         [70 -20] "sales" {:text-anchor "middle"}))))))

(defn heatmap-polygon
  "Single polygon component, performs heatmap coloring for non-selected boroughs."
  [sel hover-fn heatmap key]
  (fn [{:keys [id points] :as item}]
    [:polygon
     {:on-mouse-over #(hover-fn id)
      :key           id
      :points        points
      :fill          (if (not= sel id) (heatmap (key item)) "#333")}]))

(defn svg-heatmap
  "Main heatmap component"
  [data selected min max hover heatmap-id heatmap-key]
  (let [heatmap (heatmap-color min max heatmap-id)]
    (svg/svg
     {:width "100%" :viewBox "0 0 960 720" :stroke (col/rgba [0 0 0 0.2])}
     (doall (map (heatmap-polygon selected hover heatmap heatmap-key) (vals data)))
     (map-overlay data selected))))

(defn stats-totals
  "Total stats table component"
  []
  (let [totals (state/subscribe [:boroughs :totals])]
    (fn []
      (let [{:keys [num avg min max]} @totals]
        [:div.row
         [:div.col-xs-12
          [:table.table.table-condensed
           [:tbody
            [:tr
             [:th "Total sales:"]
             [:td (utils/format-decimal num 0 "," "")]]
            [:tr
             [:th "Total average price:"]
             [:td "£" (utils/format-gbp avg)]]
            [:tr
             [:th "Total min price:"]
             [:td "£" (utils/format-gbp min)]]
            [:tr
             [:th "Total max price:"]
             [:td "£" (utils/format-gbp max)]]]]]]))))

(defn borough-charts
  []
  (let [charts   (state/subscribe :charts)
        boroughs (state/subscribe [:boroughs :data])]
    (fn []
      [:div
       [:h2 "Property sales per borough"]
       (let [boroughs @boroughs]
         (->> @charts
              (map
               (fn [[id chart]]
                 [:div.col-xs-12.col-md-4 {:key (str id "-chart")}
                  [:div.thumbnail
                   chart
                   [:div.caption
                    [:h4 (get-in boroughs [id :name])]]]]))
              (partition-all 3)
              (map-indexed
               (fn [i row]
                 (into [:div.row {:key (str "stats-row-" i)}] row)))))])))

(defn heatmap-view
  "Main component for map view/route."
  []
  (let [boroughs    (state/subscribe :boroughs)
        heatmap-id  (state/subscribe :heatmap-id)
        heatmap-key (state/subscribe :heatmap-key)]
    (fn []
      (let [{:keys [data selected min max]} @boroughs]
        [:div.container
         [:div.row
          [:div.col-xs-6
           [:h1 "London heatmap"]]
          [:div.col-xs-3.form-align
           [dd/dropdown
            "dd-hm-type"
            @heatmap-id
            #(state/set-heatmap-key (utils/event-value-id %))
            state/heatmap-types]]
          [:div.col-xs-3.form-align
           [dd/dropdown
            "dd-hm-gradient"
            @heatmap-id
            #(state/set-heatmap-id (utils/event-value-id %))
            heatmaps]]]
         [:div.row
          [:div.col-xs-12
           [svg-heatmap
            data selected min max
            state/select-borough
            @heatmap-id
            @heatmap-key]]]
         [stats-totals]
         [borough-charts]]))))
