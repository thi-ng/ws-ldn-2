(ns ws-ldn-2.router
  "Declarative HTML5 history based SPA router with optional route
  param coercion and validation."
  (:require-macros
   [cljs-log.core :refer [debug info warn]])
  (:require
   [thi.ng.validate.core :as v]
   [clojure.string :as str]
   [goog.events :as events]
   [goog.history.EventType :as EventType])
  (:import
   [goog.history Html5History]))

(defonce history (doto (Html5History.) (.setUseFragment true)))

(defn route-for-id
  "Takes vector of routes and route id to find, returns route spec."
  [routes id] (some #(if (= id (:id %)) %) routes))

(defn format-route
  "Takes a route spec map and map of params, returns formatted URI path"
  [route params]
  (->>  (:match route)
        (reduce
         (fn [acc x] (conj acc (if (keyword? x) (params x) x)))
         [])
        (str/join "/")))

(defn format-route-for-id
  "Composition of route-for-id and format-route."
  [routes id args]
  (if-let [route (some #(if (= id (:id %)) %) routes)]
    (format-route route args)))

;; Private helper fns

(defn- match-route*
  [curr route]
  (if (= (count curr) (count route))
    (reduce
     (fn [acc [a b]]
       (cond
         (= a b)      acc
         (keyword? b) (assoc acc b a)
         :else        (reduced nil)))
     {} (partition 2 (interleave curr route)))))

(defn- coerce-route-params
  [specs params]
  (reduce
   (fn [params [k {:keys [coerce]}]]
     (if coerce
       (if-let [pv (try (coerce (params k)) (catch js/Error e))]
         (assoc params k pv)
         (reduced nil))
       params))
   params specs))

(defn- validate-route-params
  [specs params]
  (if-let [params (coerce-route-params specs params)]
    (let [valspecs (filter #(comp :validate val) specs)]
      (if (seq valspecs)
        (let [[params err] (->> valspecs
                                (reduce #(assoc % (key %2) (:validate (val %2))) {})
                                (v/validate params))]
          (if-not err params))
        params))))

(defn- match-route
  [routes curr user-fn]
  (some
   (fn [{:keys [match auth validate] :as spec}]
     ;;(debug :match match curr)
     (if (or (not auth) (user-fn curr))
       (if-let [params (match-route* curr match)]
         (if-let [params (if validate (validate-route-params validate params) params)]
           (assoc spec :params params)))))
   routes))

(defn- split-token
  [token]
  (let [items (str/split token "/")]
    (if-let [i (some (fn [[i x]] (if (#{"http:" "https:"} x) i)) (map-indexed vector items))]
      (concat (take i items) [(str/join "/" (drop i items))])
      items)))

;; Main API

(defn start!
  "Takes vector of route specs, init URI path, default route spec,
  handler fn and auth fn.

  - init URI can be used to force a certain entry route
  - handler fn is called on nav change with matched route spec
  - auth fn also called with single route spec during matching and
    can be used to restrict access to routes (e.g. if user not logged
    in etc.). Only called for routes with :auth key enabled and must
    return truthy value for route to succeed"
  [routes init-uri default-route dispatch-fn user-fn]
  (info "starting router...")
  (let [init-uri (volatile! init-uri)]
    (doto history
      (events/listen
       EventType/NAVIGATE
       (fn [e]
         (if-let [init @init-uri]
           (do (vreset! init-uri nil)
               (.setToken history init))
           (let [token  (.-token e)
                 route  (split-token token)
                 route' (match-route routes route user-fn)]
             ;;(debug :route route :token token :id (:id route'))
             (if route'
               (dispatch-fn route')
               (.setToken history (format-route default-route {})))))))
      (.setEnabled true))))

(defn trigger!
  "Takes URI path and sets as history token."
  [uri]
  (.setToken history uri))

(defn virtual-link
  "Helper :on-click handler for internal SPA links. Calls
  preventDefault on event and calls trigger with given URI path."
  [uri] (fn [e] (.preventDefault e) (trigger! uri)))
