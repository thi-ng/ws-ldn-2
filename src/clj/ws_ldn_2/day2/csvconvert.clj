(ns ws-ldn-2.day2.csvconvert
  (:require
   [thi.ng.dstruct.core :as d]
   [thi.ng.fabric.facts.core :as ff]
   [thi.ng.strf.core :as f]
   [clojure.data.csv :as csv]
   [clojure.java.io :as io])
  (:import
   [java.text SimpleDateFormat]))

(def df (SimpleDateFormat. "dd-MMM-yy"))

(defn load-csv-resource
  "Takes CSV source (path, URI or stream) and returns parsed CSV as vector of rows"
  [path]
  (let [data (-> path
                 (io/reader)
                 (csv/read-csv :separator \,))]
    (println (count data) "rows loaded")
    data))

(defn build-column-index
  "Takes a set of columns to be indexed and first row of CSV (column headers).
  Returns map of {id col-name, ...} for all matched columns."
  [wanted-columns csv-columns]
  (->> csv-columns
       (map (fn [i x] [i x]) (range))
       (filter #(wanted-columns (second %)))
       (into {})))

(defn transform-csv-row
  [col-idx keep-cols ftx row]
  (->> row
       (map-indexed (fn [i x] [i x]))
       (filter (fn [[i]] (keep-cols i)))
       (map
        (fn [[i x]]
          (let [id (keyword (col-idx i))]
            [id (if-let [f (ftx id)] (f x) x)])))
       (filter (fn [[_ x]] (if (string? x) (seq x) (not (nil? x)))))
       (into {})))

(defn mapped-csv
  ([path cols field-tx]
   (mapped-csv path cols field-tx 1e9))
  ([path cols field-tx limit]
   (let [rows      (take (inc limit) (load-csv-resource path))
         col-idx   (build-column-index cols (first rows))
         keep-cols (set (keys col-idx))]
     (map #(transform-csv-row col-idx keep-cols field-tx %) (rest rows)))))

(defn sale->graph
  [sale]
  (ff/map->facts
   {(:transaction_id sale)
    {"rdf:type"             "schema:TradeAction"
     "schema:price"         (:price sale)
     "schema:priceCurrency" "GBP"
     "schema:postalCode"    (:post_code sale)
     "schema:purchaseDate"  (:date_processed sale)
     "ws:onsID"             (:borough_code sale)
     "ws:propertyType"      (:property_type sale)}}))

(defn load-house-sales
  [path]
  (mapped-csv
   path
   #{"transaction_id" "price" "date_processed" "post_code" "property_type" "borough_code"}
   {:transaction_id #(subs % 1 (dec (count %)))
    :price          #(f/parse-int % 10)
    :date_processed #(.parse df %)}))

(comment
  ;; CSV column fields
  #{"transaction_id" "price" "date_processed" "post_code" "postcode" "property_type" ",whether_newbuild,tenure,address2,address4,town,local_authority,county,record_status,year,month,quarter,house_flat,statsward,oa11,lsoa11,msoa11,inner_outer,year_month,Postcode_sector,Postcode_district,Ward14,ward_code,borough_code,borough_name"}
  )

;; (def sales (load-house-sales (io/resource "data/london-sales-2013-2014.csv")))
