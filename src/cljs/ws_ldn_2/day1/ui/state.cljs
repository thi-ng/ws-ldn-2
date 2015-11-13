(ns ws-ldn-2.day1.ui.state
  (:require-macros
   [reagent.ratom :refer [reaction]]
   [cljs-log.core :refer [debug info warn]])
  (:require
   [reagent.core :as r]
   [thi.ng.strf.core :as f]
   [thi.ng.geom.core.vector :refer [vec2]]
   [thi.ng.geom.svg.core :as svg]
   [thi.ng.math.core :as m]
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

(defn lat-log
  [lat] (Math/log (Math/tan (+ (/ (m/radians lat) 2) m/QUARTER_PI))))

(defn mercator-in-rect
  [[lon lat] [left right top bottom] w h]
  (let [lon              (m/radians lon)
        left             (m/radians left)
        [lat top bottom] (map lat-log [lat top bottom])]
    (vec2
      (* w (/ (- lon left) (- (m/radians right) left)))
      (* h (/ (- lat top) (- bottom top))))))

(defn process-borough
  [borough]
  (let [poly   (first (borough '?apoly))
        poly   (map f/parse-float (clojure.string/split poly #" "))
        points (map vec2 (partition 2 poly))]
    (assoc borough '?apoly
           (mapv #(mercator-in-rect (:yx %) [-0.6 0.5 51.75 51.2] 960 720) points))))

(defn parse-polygons
  [boroughs]
  (set-state! :boroughs (map (comp process-borough first val) boroughs)))

(defn parse-query-response
  [{:keys [body]}]
  (if (-> body vals ffirst (get '?apoly))
    (do (info :parse) (parse-polygons body))
    (info :no-polies)))

(defn submit-query
  []
  (io/request
   {:uri     "http://localhost:8000/query"
    :method  :post
    ;;:params  {:limit 1000 :offset 1000}
    :data    {:spec (:query @app-state)}
    :success (fn [status data]
               (info :response data)
               (set-state! :raw-result data)
               (parse-query-response data))
    :error   (fn [status msg] (warn :error status msg))}))

(defn set-viz-query
  []
  (->> {:spec (:query @app-state) :format "png"}
       (io/->request-data)
       (str "http://localhost:8000/queryviz?")
       (set-state! :query-viz-uri)))

(defn select-borough
  [id]
  (set-state! :selected-borough id))

(defn init-map
  [] (debug :init-map))
