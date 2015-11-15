(ns ws-ldn-2.state
  (:require-macros
   [reagent.ratom :refer [reaction]]
   [cljs-log.core :refer [debug info warn]])
  (:require
   [ws-ldn-2.map-project :as mp]
   [reagent.core :as r]
   [thi.ng.strf.core :as f]
   [thi.ng.geom.core.vector :as v]
   [thi.ng.geom.core.utils :as gu]
   [thi.ng.geom.svg.core :as svg]
   [thi.ng.math.core :as m]
   [thi.ng.domus.io :as io]
   [clojure.string :as str]))

(declare compute-borough-stats)

(def query-presets
  {:custom
   {:query "{:prefixes {}
 :q [{:where []}]
 :select :*}"
    :label "Custom"}

   :boroughs
   {:query "{:prefixes {\"sg\" \"http://statistics.data.gov.uk/def/statistical-geography#\"}
 :q [{:where [[?s \"rdf:type\" \"schema:TradeAction\"]
              [?s \"schema:price\" ?price]
              [?s \"schema:purchaseDate\" ?date]
              [?s \"schema:postalCode\" ?zip]
              [?s \"ws:onsID\" ?boroughID]
              [?borough \"rdfs:label\" ?boroughID]
              [?borough \"sg:officialName\" ?name]
              [?borough \"sg:hasExteriorLatLongPolygon\" ?poly]]}]
 :aggregate {?num (agg-count ?s)
             ?avg (agg-avg ?price)
             ?min (agg-min ?price)
             ?max (agg-max ?price)
             ?apoly (agg-collect ?poly)
             ?aname (agg-collect ?name)}
 :group-by ?boroughID
 :select [?boroughID ?apoly ?avg ?min ?max ?num ?aname]}"
    :label "London boroughs (polygons)"}

   :single-borough
   {:query "{:prefixes {\"sg\" \"http://statistics.data.gov.uk/def/statistical-geography#\"}
 :q [{:where [[?s \"rdf:type\" \"schema:TradeAction\"]
              [?s \"schema:price\" ?price]
              [?s \"schema:purchaseDate\" ?date]
              [?s \"ws:onsID\" ?boroughID]
              [?borough \"rdfs:label\" ?boroughID]]}]
 :filter   (= \"{BOROUGH_ID}\" ?boroughID)
 :order ?date
 :select [?price ?date]}"
    :label "House prices (single borough)"}})

(def heatmap-types
  {:avg {:label "Average sale price"}
   :num {:label "Number of sales"}})

(defonce app-state (r/atom {}))

(defn set-state!
  [key val]
  (info key val)
  (swap! app-state (if (sequential? key) assoc-in assoc) key val))

(defn update-state!
  [key f & args]
  (info key args)
  (swap! app-state #(apply (if (sequential? key) update-in update) % key f args)))

(defn subscribe
  [key]
  (if (sequential? key)
    (reaction (get-in @app-state key))
    (reaction (@app-state key))))

(defn submit-registered-query
  [id success-fn & opts]
  (io/request
   {:uri     (str "/queries/" (name id))
    :method  :get
    :data    (apply hash-map opts)
    :success (fn [status data]
               (set-state! :raw-query-result data)
               (success-fn data))
    :error   (fn [status msg] (warn :error status msg))}))

(defn submit-oneoff-query
  [q success-fn & opts]
  (io/request
   {:uri     "/query"
    :method  :post
    :params  (apply hash-map opts)
    :data    {:spec q}
    :success (fn [status data]
               (set-state! :raw-query-result data)
               (success-fn data))
    :error   (fn [status msg] (warn :error status msg))}))

(defn nav-change
  [route]
  (set-state! :curr-route route)
  (set-state! :nav-collapsed? false))

(defn nav-toggle-collapse
  [] (update-state! :nav-collapsed? not))

(defn set-query
  [q]
  (set-state! :query q)
  (set-state! :query-preset nil))

(defn set-viz-query
  []
  (->> {:spec (:query @app-state) :format "png"}
       (io/->request-data)
       (str "/queryviz?")
       (set-state! :query-viz-uri)))

(defn set-heatmap-id
  [id] (set-state! :heatmap-id (keyword id)))

(defn set-heatmap-key
  [id]
  (set-state! :heatmap-key (keyword id))
  (compute-borough-stats))

(defn apply-query-preset
  [id]
  (set-state! :query (get-in query-presets [id :query]))
  (set-state! :query-preset id))

(defn compute-borough-stats
  []
  (let [boroughs (-> @app-state :boroughs :data vals)
        key      (:heatmap-key @app-state)]
    (update-state!
     :boroughs merge
     {:min       (reduce min (map key boroughs))
      :max       (reduce max (map key boroughs))
      :total-min (reduce min (map :min boroughs))
      :total-max (reduce max (map :max boroughs))
      :total-avg (/ (reduce + (map :avg boroughs)) (count boroughs))
      :total-num (reduce + (map :num boroughs))})))

(defn process-borough
  [{:syms [?apoly ?avg ?min ?max ?num ?boroughID ?aname]}]
  (let [poly     (first ?apoly)
        poly     (map f/parse-float (str/split poly #" "))
        points   (map v/vec2 (partition 2 poly))
        points   (mapv #(mp/mercator-in-rect (:yx %) [-0.52 0.35 51.7 51.25] 960 720) points)
        centroid (gu/centroid points)
        points   (apply f/format (svg/point-seq-format (count points)) points)]
    {:id       ?boroughID
     :name     (first ?aname)
     :points   points
     :centroid centroid
     :avg      ?avg
     :num      ?num
     :min      ?min
     :max      ?max}))

(defn parse-boroughs
  [boroughs]
  (let [boroughs (map (comp process-borough first val) boroughs)]
    (set-state! :boroughs {:data (into (sorted-map) (zipmap (map :id boroughs) boroughs))})
    (compute-borough-stats)))

(defn parse-boroughs-query-response
  [response]
  (parse-boroughs (:body response)))

(defn parse-query-response
  [response])

(defn parse-borough-prices-response
  [id]
  (fn [{:keys [body]}]
    (info :response body)
    (set-state! [:query-cache id] body)
    (set-state! :selected-borough-details body)))

(defn select-borough
  [id]
  (set-state! [:boroughs :selected] id)
  (if-let [cached (get-in @app-state [:query-cache id])]
    (set-state! :selected-borough-details cached)
    (let [q (get-in query-presets [:single-borough :query])
          q (str/replace q #"\{BOROUGH_ID\}" id)]
      (set-state! [:query-cache id] [])
      (submit-oneoff-query q (parse-borough-prices-response id) :limit 1000))))

(defn init-app
  []
  (swap! app-state merge
         {:query        (get-in query-presets [:boroughs :query])
          :query-preset :boroughs
          :query-cache  {}
          :heatmap-id   :yellow-magenta-cyan
          :heatmap-key  :avg
          :inited       true})
  (submit-registered-query
   :boroughs parse-boroughs-query-response))
