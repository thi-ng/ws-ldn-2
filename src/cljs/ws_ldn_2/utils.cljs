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

(defn format-currency
  [x prec thousands frac]
  (let [s (if (neg? x) "-" "")
        x (.toFixed (Math/abs x) prec)
        i (str (int x))
        j (count i)
        j (if (> j 3) (rem j 3) 0)]
    (str s
         (if (pos? j) (str (subs i 0 j) thousands))
         (str/replace (subs i j) #"(\d{3})(?=\d)" (str "$1" thousands))
         (str frac (subs (.toFixed (Math/abs (- x i)) prec) 2)))))


;; Number.prototype.formatMoney = function(c, d, t){
;; var n = this, 
;;     c = isNaN(c = Math.abs(c)) ? 2 : c, 
;;     d = d == undefined ? "." : d, 
;;     t = t == undefined ? "," : t, 
;;     s = n < 0 ? "-" : "", 
;;     i = parseInt(n = Math.abs(+n || 0).toFixed(c)) + "", 
;;     j = (j = i.length) > 3 ? j % 3 : 0;
;;    return s + (j ? i.substr(0, j) + t : "") + i.substr(j).replace(/(\d{3})(?=\d)/g, "$1" + t) + (c ? d + Math.abs(n - i).toFixed(c).slice(2) : "");
;;  };
