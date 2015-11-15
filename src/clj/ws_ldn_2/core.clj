(ns ws-ldn-2.core
  (:require
   [ws-ldn-2.utils :as utils]
   [thi.ng.fabric.ld.core :as ld]
   [thi.ng.fabric.facts.queryviz :as qviz]
   [thi.ng.validate.core :as v]
   [compojure.core :as compojure :refer [GET POST DELETE]]
   [com.stuartsierra.component :as comp]
   [ring.util.response :as resp]
   [ring.util.mime-type :as mime]
   [ring.middleware.cors :refer [wrap-cors]]
   [clojure.java.shell :refer [sh]]
   [clojure.java.io :as io]
   [clojure.tools.namespace.repl :refer (refresh)]
   [taoensso.timbre :refer [debug info warn]])
  (:import
   [java.io StringBufferInputStream ByteArrayInputStream]))

;; additional HTTP handlers

(defn queryviz-handler
  "Higher order handler fn for visualizing fabric query spec using Graphviz dot.
  Fn is first called during system setup and provided with graph
  model, prefix, query and inference rule registries. Returns actual handler fn."
  [model prefixes queries rules]
  (fn [req]
    (ld/validating-handler
     req
     ;; request param coercions
     {:spec   :edn}
     ;; request param validation spec
     {:spec   :query
      :format (v/member-of #{"png" "jpg" "svg"})}
     ;; actual handler (only executed if validation succeeds)
     (fn [_ {:keys [spec format] :as params}]
       (let [query     (ld/transform-query model spec)
             ;; generate graphviz source from query spec
             ;; http://graphviz.org/pdf/dotguide.pdf
             dot       (qviz/query->graphviz query)
             ;; call graphviz dot shell command with generated string
             ;; as input and capture output as byte array
             ;; http://clojuredocs.org/clojure.java.shell/sh
             img-bytes (:out (sh "dot"
                                 (str "-T" format)
                                 :in (StringBufferInputStream. dot)
                                 :out-enc :bytes))]
         ;; response map with image bytes as input stream
         ;; mime type set based on given image format
         ;; https://github.com/ring-clojure/ring
         ;; https://github.com/ring-clojure/ring/blob/master/SPEC
         {:status  200
          :body    (ByteArrayInputStream. img-bytes)
          :headers {"Content-type" (mime/default-mime-types format)}})))))

;; component system lifecycle control fns

(def system "Component system map" nil)

(defn init
  "Initializes custom fabric.ld component system based on default config,
  but with different data and extra queries, handlers"
  []
  (let [config (merge-with
                utils/deep-merge
                (ld/default-config)
                {:graph   {:import ^:replace [(io/resource "data/london-boroughs.nt")
                                              (io/resource "data/sales-2013-xl.edn")]}
                 :queries {:specs (read-string (slurp (io/resource "data/queries.edn")))}
                 :handler {:inject-routes [[:get "/queryviz" queryviz-handler]
                                           [:get "/" (constantly
                                                      (fn [req]
                                                        {:status 302
                                                         :header {"Location" "/index.html"}}))]]}
                 :log     {:fn (fn [_ _] (fn [_]))}})]
    (taoensso.timbre/set-level! :info)
    (clojure.pprint/pprint config)
    (alter-var-root #'system (constantly (ld/make-system config)))))

(defn start []
  (alter-var-root #'system comp/start))

(defn stop []
  (alter-var-root #'system (fn [s] (when s (comp/stop s)))))

(defn launch []
  (init)
  (start))

(defn reset []
  (stop)
  (refresh :after 'ws-ldn-2.core/launch))
