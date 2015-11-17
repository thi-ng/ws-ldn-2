(ns ws-ldn-2.state
  (:require-macros
   [reagent.ratom :refer [reaction]]
   [cljs-log.core :refer [debug info warn]]
   [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [ws-ldn-2.map-project :as mp]
   [ws-ldn-2.utils :as utils]
   [reagent.core :as r]
   [thi.ng.strf.core :as f]
   [thi.ng.geom.core.vector :as v]
   [thi.ng.geom.core.utils :as gu]
   [thi.ng.geom.svg.core :as svg]
   [thi.ng.geom.svg.adapter :as svgadapt]
   [thi.ng.geom.viz.core :as viz]
   [thi.ng.color.core :as col]
   [thi.ng.math.core :as m]
   [thi.ng.domus.io :as io]
   [cljs.core.async :as async]
   [clojure.string :as str]))

(def query-presets
  "Presets for query editor component"
  {:custom
   {:query "{:prefixes {\"sg\" \"http://statistics.data.gov.uk/def/statistical-geography#\"}
 :q        [{:where [[?s ?p ?o]]}]
 :order    ?s
 :select   :*
}"
    :label "Custom"}

   :boroughs
   {:query "{:prefixes  {\"sg\" \"http://statistics.data.gov.uk/def/statistical-geography#\"}
 :q         [{:where [[?s \"rdf:type\" \"schema:SellAction\"]
                      [?s \"schema:price\" ?price]
                      [?s \"schema:purchaseDate\" ?date]
                      [?s \"schema:postalCode\" ?zip]
                      [?s \"ws:onsID\" ?boroughID]
                      [?borough \"rdfs:label\" ?boroughID]
                      [?borough \"sg:officialName\" ?name]
                      [?borough \"sg:hasExteriorLatLongPolygon\" ?poly]]}]
 :aggregate {?num   (agg-count ?s)
             ?avg   (agg-avg ?price)
             ?min   (agg-min ?price)
             ?max   (agg-max ?price)
             ?apoly (agg-collect ?poly)
             ?aname (agg-collect ?name)}
 :group-by  ?boroughID
 :select    [?boroughID ?apoly ?avg ?min ?max ?num ?aname]
}"
    :label "London boroughs (polygons)"}

   :single-borough
   {:query "{:prefixes {\"sg\" \"http://statistics.data.gov.uk/def/statistical-geography#\"}
 :q        [{:where [[?s \"rdf:type\" \"schema:SellAction\"]
                     [?s \"schema:price\" ?price]
                     [?s \"schema:purchaseDate\" ?date]
                     [?s \"ws:onsID\" ?boroughID]
                     [?borough \"rdfs:label\" ?boroughID]]}]
 :filter   (= \"E09000001\" ?boroughID)
 :order    ?date
 :select   [?price ?date]
}"
    :label "House prices (single borough)"}})

(def heatmap-types
  "Available heatmap types"
  {:avg {:label "Average sale price"}
   :num {:label "Number of sales"}})

(def init-states
  "Map of init states and their descriptions (used during start up,
  see app-component in core ns"
  {:init             "Initializing..."
   :process-polygons "Processing polygons..."
   :generate-charts  "Generating charts..."})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Global application state & helpers
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce app-state (r/atom {}))

(defn set-state!
  "Helper fn to reset app-state for given key. Key can be single
  keyword or path vector."
  [key val]
  (debug key val)
  (swap! app-state (if (sequential? key) assoc-in assoc) key val))

(defn update-state!
  "Helper fn to update app-state value for given key. Key can be single
  keyword or path vector. Behaves like clojure.core/update or update-in"
  [key f & args]
  (debug key args)
  (swap! app-state #(apply (if (sequential? key) update-in update) % key f args)))

(defn subscribe
  "Helper fn to create a reagent reaction for given app-state key/path."
  [key]
  (if (sequential? key)
    (reaction (get-in @app-state key))
    (reaction (@app-state key))))

;; TODO add error handling
(defn do-request
  "Helper fn to perform XHR request. Takes request options map and
  core.async channel. On successful response, places response data on
  channel."
  [spec ch]
  (->> {:success (fn [status data]
                   (set-state! :raw-query-result data)
                   (async/put! ch data))
        :error   (fn [status msg] (warn :error status msg))}
       (merge spec)
       (io/request)))

(defn submit-registered-query
  "Submits GET request to a registered fabric.ld query. Accepts query
  id, result channel and any number of keyword options (form params)."
  [id ch & opts]
  (do-request
   {:uri    (str "/queries/" (name id))
    :data   (apply hash-map opts)
    :method :get}
   ch))

(defn submit-oneoff-query
  "Submits POST request to execute a custom fabric.ld query. Accepts
  query spec, result channel and any number of keyword options (querystring params)."
  [q ch & opts]
  (do-request
   {:uri    "/query"
    :params (apply hash-map opts)
    :data   {:spec q}
    :method :post}
   ch))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Application state update handlers
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn nav-change
  [route]
  (set-state! :curr-route route)
  (set-state! :nav-open? false))

(defn nav-toggle-collapse
  [] (update-state! :nav-open? not))

(defn set-init-state
  ([id]
   (set-state! :init-state {:id id}))
  ([id progress total]
   (set-state! :init-state {:id id :progress progress :total total})))

(defn update-init-state-progress
  [] (update-state! [:init-state :progress] inc))

(defn set-query
  [q] (set-state! :query q))

(defn set-query-preset
  [id]
  (set-state! :query (get-in query-presets [id :query]))
  (set-state! :query-preset id))

(defn set-viz-query
  "Formats current user query as URL for /queryviz server handler and
  sets :query-viz-uri in app-state. This value is picked up by
  queryviz component and used as image src attrib. When image is added
  to DOM, the browser then executes the request automatically."
  []
  (->> {:spec (:query @app-state) :format "png"}
       (io/->request-data)
       (str "/queryviz?")
       (set-state! :query-viz-uri))
  (set-state! :user-query-results nil))

(defn transform-user-query-results
  "Helper fn to transform query results for populating the result
  table component. Inspects returned query spec to handle both
  grouped/ungrouped results."
  [results]
  (let [grouped (get-in results [:spec :group-by])
        body    (:body results)
        qvars   (if grouped
                  (->> grouped
                       (disj (into (sorted-set) (keys (first (val (first body))))))
                       (vec)
                       (cons grouped)
                       (vec))
                  (vec (into (sorted-set) (keys (first body)))))
        body    (if grouped
                  (mapcat (fn [[k vals]] (map #(assoc % grouped k) vals)) body)
                  body)]
    {:grouped grouped
     :qvars   qvars
     :body    body}))

(defn submit-user-query
  "Async handler to submit current user query for execution and then
  later processing/transformation of results for table display. Also
  kills query-viz state, thus acting as display toggle between table &
  graphviz visualization."
  []
  (let [ch (async/chan)]
    (submit-oneoff-query (:query @app-state) ch)
    (set-state! :query-viz-uri nil)
    (go
      (when-let [results (async/<! ch)]
        (->> results
             (transform-user-query-results)
             (set-state! :user-query-results))))))

(defn set-heatmap-id
  [id] (set-state! :heatmap-id (keyword id)))

(defn set-heatmap-key
  [id] (async/put! (:borough-stats-chan @app-state) (keyword id)))

(defn select-borough
  [id] (set-state! [:boroughs :selected] id))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Async handlers (mainly for app initialization)
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn process-borough
  "Helper fn to transform a single borough query result. Performs map
  projection of borough's lat/lon polygon points and constructs
  pre-formatted SVG points, returns transformed map."
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

(defn async-borough-processor
  "Called from async-initial-query-processor with response from
  initial borough query. In a go block transforms borough details and
  polygons, then triggers aggregate stats computation for heatmap."
  [results]
  (go
    (set-init-state :process-polygons)
    ;; allow DOM update
    (async/<! (async/timeout 17))
    (let [boroughs (map (comp process-borough first val) (:body results))
          boroughs (into (sorted-map) (zipmap (map :id boroughs) boroughs))]
      (set-state! :boroughs {:data boroughs})
      (set-heatmap-key (:heatmap-key @app-state)))))

(defn borough-price-chart
  "Helper fn to construct the price chart for a single borough. Takes
  single map entry of [borough-id query-results] and replaces value
  with generated SVG DOM fragment."
  [[id results]]
  (let [data  (mapv #(% '?price) results)
        len   (count data)
        min-v (reduce min data)
        max-v (reduce max data)
        maj-x (if (> len 200) 200 50)
        spec  {:x-axis (viz/linear-axis
                        {:domain      [0 len]
                         :range       [40 220]
                         :major       maj-x
                         :minor       (/ maj-x 2)
                         :pos         100
                         :label       (viz/default-svg-label int)
                         :label-style {:style {:font-size "9px"}}
                         :attribs     {:stroke-width "0.5px"}})
               :y-axis (viz/log-axis
                        {:domain      [min-v max-v]
                         :range       [100 5]
                         :pos         40
                         :label-dist  15
                         :label       (viz/default-svg-label utils/format-k)
                         :label-style {:style {:font-size "9px"} :text-anchor "end"}
                         :label-y     3
                         :attribs     {:stroke-width "0.5px"}})
               :grid   {:attribs {:stroke "#ccc" :stroke-width "0.5px"}
                        :minor-y true}
               :data   [{:values  (map-indexed vector data)
                         :attribs {:fill "none" :stroke "url(#grad)" :stroke-width "0.5px"}
                         :layout  viz/svg-line-plot}]}]
    [id (->> spec
             (viz/svg-plot2d-cartesian)
             (svg/svg
              {:width "100%" :viewBox "0 0 240 120"}
              (svg/linear-gradient
               "grad" {:gradientTransform "rotate(90)"}
               [0 (col/css "#f03")] [1 (col/css "#0af")]))
             (svgadapt/inject-element-attribs svgadapt/key-attrib-injector))]))

(defn async-chart-generator
  "Takes query results and async channel. In a go-loop, constructs
  charts for all boroughs and on completion, places `true` on result
  channel. Updates :init-state-progress value during execution."
  [results done]
  (let [ch (async/chan)]
    (go-loop []
      (if-let [borough (async/<! ch)]
        (do (update-init-state-progress)
            (update-state! :charts conj (borough-price-chart borough))
            ;; slight delay between iterations to allow DOM updates
            (async/<! (async/timeout 17))
            (recur))
        (async/>! done true)))
    (set-init-state :generate-charts 0 (count results))
    (async/onto-chan ch results)))

(defn async-borough-stats-processor
  "Defines sync handler to compute aggregate values for a given
  heatmap type placed on handler's channel. Fn is called from init-app
  and returns channel which is stored in app-state atom. Channel is
  later used by set-heatmap-key handler to trigger new heatmap type
  and computation in this handler."
  []
  (let [ch (async/chan)]
    (go-loop []
      (when-let [key (async/<! ch)]
        (let [boroughs (-> @app-state :boroughs :data vals)]
          (set-state! :heatmap-key key)
          (update-state!
           :boroughs merge
           {:min    (reduce min (map key boroughs))
            :max    (reduce max (map key boroughs))
            :totals {:min (reduce min (map :min boroughs))
                     :max (reduce max (map :max boroughs))
                     :avg (/ (reduce + (map :avg boroughs)) (count boroughs))
                     :num (reduce + (map :num boroughs))}})
          (recur))))
    ch))

(defn async-initial-query-processor
  "Async initialization handler. Executes initial boroughs query,
  trigger result processing, triggers price detail query and then
  coordinates with aync-chart-processor, finally sets :ready?
  app-state key to enable full UI."
  []
  (let [ch (async/chan)]
    (go
      (submit-registered-query :boroughs ch)
      (when-let [resp (async/<! ch)]
        (async-borough-processor resp)
        (submit-registered-query :borough-prices ch)
        (when-let [resp (async/<! ch)]
          (async-chart-generator (:body resp) ch)
          (async/<! ch)
          (set-state! :app-ready? true))))
    ch))

(defn init-app
  []
  (swap! app-state merge
         {:query              (get-in query-presets [:boroughs :query])
          :query-preset       :boroughs
          :heatmap-id         :yellow-magenta-cyan
          :heatmap-key        :avg
          :inited             true
          :init-state         :init
          :charts             (sorted-map)
          :borough-stats-chan (async-borough-stats-processor)})
  (async-initial-query-processor))
