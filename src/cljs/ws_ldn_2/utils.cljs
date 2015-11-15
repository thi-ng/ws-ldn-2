(ns ws-ldn-2.utils
  (:require
   [clojure.string :as str]
   [thi.ng.strf.core :as f]))

(defn aget-in
  "Looks up dotted path (as str) in JS object."
  [obj path]
  (loop [obj obj, path (str/split path ".")]
    (if path
      (recur (aget obj (first path)) (next path))
      obj)))

(defn event-value-id
  [e] (-> e .-target .-value keyword))

(defn format-decimal
  [x prec thousands frac]
  (let [x' (str/replace (.toFixed (Math/abs x) prec) "." frac)]
    (str
     (if (neg? x) "-")
     (str/replace x' #"(\d)(?=(\d{3})+([^\d]|$))" (str "$1" thousands)))))

(defn format-gbp [x] (format-decimal x 2 "," "."))
