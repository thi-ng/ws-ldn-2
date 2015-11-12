(defproject thi.ng/ws-ldn-2 "0.1.0-SNAPSHOT"
  :description   "thi.ng workshop #2"
  :url           "http://thi.ng"
  :license       {:name "Apache Software License 2.0"
                  :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies  [[org.clojure/clojure "1.7.0"]
                  [org.clojure/clojurescript "1.7.170"]
                  [org.clojure/core.async "0.2.371"]
                  [thi.ng/geom "0.0.908"]
                  [thi.ng/fabric "0.0.376"]
                  [thi.ng/validate "0.1.3"]
                  [cljsjs/codemirror "5.7.0-3"]
                  [reagent "0.5.1"]
                  [cljs-log "0.2.2"]
                  [criterium "0.4.3"]]

  :plugins       [[lein-figwheel "0.5.0-SNAPSHOT"]
                  [lein-cljsbuild "1.1.1"]
                  [lein-environ "1.0.0"]]

  :source-paths  ["src/clj" "src/cljs"]

  :clean-targets ^{:protect false} ["target" "resources/public/js"]

  :profiles      {:prod {:env {:log-level 4}}}

  :cljsbuild     {:builds [{:id           "day1"
                            :source-paths ["src/cljs"]
                            :figwheel     {:on-jsload "ws-ldn-2.day1.ui.core/on-js-reload"}
                            :compiler     {:main                 ws-ldn-2.day1.ui.core
                                           :optimizations        :none
                                           :asset-path           "js/out"
                                           :output-to            "resources/public/js/app.js"
                                           :output-dir           "resources/public/js/out"
                                           :source-map           true
                                           :source-map-timestamp true
                                           :cache-analysis       true}}
                           {:id           "day1-min"
                            :source-paths ["src/cljs"]
                            :compiler     {:main          ws-ldn-2.day1.ui.core
                                           :optimizations :advanced
                                           :pretty-print  false
                                           :asset-path    "js/day1"
                                           :output-to     "resources/public/js/app.js"}}]}

  :figwheel      {:http-server-root "public"         ;; default and assumes "resources"
                  :server-port 3449                  ;; default
                  :css-dirs ["resources/public/css"] ;; watch and update CSS
                  })
