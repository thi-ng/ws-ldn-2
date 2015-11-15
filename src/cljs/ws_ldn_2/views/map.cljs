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
   [thi.ng.geom.svg.adapter :as svgadapt]
   [thi.ng.geom.viz.core :as viz]
   [thi.ng.color.core :as col]
   [thi.ng.color.gradients :as grad]
   [thi.ng.math.core :as m]
   [thi.ng.strf.core :as f]))

(def heatmaps
  (sorted-map
   :green-cyan          {:flipped false}
   :blue-cyan           {:flipped false}
   :green-magenta       {:flipped false}
   :orange-blue         {:flipped true}
   :rainbow1            {:flipped true}
   :yellow-magenta-cyan {:flipped true}))

(defn heatmap-color
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
  [sel hover-fn heatmap key]
  (fn [{:keys [id points] :as item}]
    [:polygon
     {:on-mouse-over #(hover-fn id)
      :key           id
      :points        points
      :fill          (if (not= sel id) (heatmap (key item)) "#333")}]))

(defn svg-map
  [data selected min max hover heatmap-id heatmap-key]
  (let [heatmap (heatmap-color min max heatmap-id)]
    (svg/svg
     {:width "100%" :viewBox "0 0 960 720" :stroke (col/rgba [0 0 0 0.2])}
     (doall (map (heatmap-polygon selected hover heatmap heatmap-key) (vals data)))
     (map-overlay data selected))))

(defn heatmap-view
  []
  (let [boroughs    (state/subscribe :boroughs)
        heatmap-id  (state/subscribe :heatmap-id)
        heatmap-key (state/subscribe :heatmap-key)
        cache (state/subscribe :query-cache)]
    (fn []
      (let [{:keys [data selected min max]} @boroughs
            cache @cache]
        [:div.container
         [:div.row
          [:div.col-xs-6
           [:h1 "London heatmap"]]
          [:div.col-xs-3 {:style {:margin-top "22px"}}
           [dd/dropdown
            @heatmap-id
            #(state/set-heatmap-key (utils/event-value-id %))
            state/heatmap-types]]
          [:div.col-xs-3 {:style {:margin-top "22px"}}
           [dd/dropdown
            @heatmap-id
            #(state/set-heatmap-id (utils/event-value-id %))
            heatmaps]]]
         [:div.row
          [:div.col-xs-12
           [svg-map
            data selected min max
            state/select-borough
            @heatmap-id
            @heatmap-key]]]
         [:div.row
          [:div.col-xs-12
           [:table.table.table-condensed
            [:tbody
             [:tr [:th "Total sales:"] [:td (utils/format-decimal (:total-num @boroughs) 0 "," "")]]
             [:tr [:th "Total average price:"] [:td "£" (utils/format-gbp (:total-avg @boroughs))]]
             [:tr [:th "Total min price:"] [:td "£" (utils/format-gbp (:total-min @boroughs))]]
             [:tr [:th "Total max price:"] [:td "£" (utils/format-gbp (:total-max @boroughs))]]]]]]
         (->> (vals (:data @boroughs))
              (map
               (fn [b]
                 [:div.col-xs-4 {:key (str (:id b) "-stats")}
                  [:div.thumbnail
                   (when-let [data (seq (cache (:id b)))]
                     (let [data  (map #(get % '?price) data)
                           len   (count data)
                           min-v (reduce cljs.core/min data)
                           max-v (reduce cljs.core/max data)
                           spec  {:x-axis (viz/linear-axis
                                           {:domain      [0 len]
                                            :range       [40 220]
                                            :major       200
                                            :minor       100
                                            :pos         100
                                            :label       (viz/default-svg-label int)
                                            :label-style {:style {:font-size "9px"}}})
                                  :y-axis (viz/log-axis
                                           {:domain      [min-v max-v]
                                            :range       [100 5]
                                            :pos         40
                                            :label-dist  15
                                            :label (viz/default-svg-label utils/format-k)
                                            :label-style {:style {:font-size "9px"} :text-anchor "end"}})
                                  :grid   {:attribs {:stroke "#ccc" :stroke-width "0.5px"}
                                           :minor-y true}
                                  :data   [{:values  (map-indexed vector data)
                                            :attribs {:fill "none" :stroke "url(#grad)" :stroke-width "0.5px"}
                                            :layout  viz/svg-line-plot}]}]
                       (->> spec
                            (viz/svg-plot2d-cartesian)
                            (svgadapt/inject-element-attribs svgadapt/key-attrib-injector)
                            (svg/svg
                             {:width "100%" :viewBox "0 0 240 120"}
                             (svg/linear-gradient
                              "grad" {:gradientTransform "rotate(90)"}
                              [0 (col/css "#f03")] [1 (col/css "#0af")])))))
                   [:div.caption
                    [:h5 (:name b)]]]]))
              (partition-all 3)
              (map-indexed
               (fn [i row] (into [:div.row {:key (str "stats-row-" i)}] row))))]))))
