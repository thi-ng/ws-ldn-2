(ns ws-ldn-2.csvconvert
  (:require
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

(defn sale->triples
  [sale]
  (ff/map->facts
   {(:transaction_id sale) {"rdf:type"             "schema:SellAction"
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
   ;; CSV column fields to extract
   #{"transaction_id" "price" "date_processed" "post_code" "property_type" "borough_code"}
   ;; CSV column transformers
   {:transaction_id #(subs % 1 (dec (count %)))
    :price          #(f/parse-int % 10)
    :date_processed #(.parse df %)}))

(defn write-triples-edn
  "Takes a file path and seq of CSV record maps, converts each map
  into a seq of triples and writes all triples to given file."
  [path xs]
  (with-open [out (io/writer path)]
    (.append out \[)
    (doseq [f (mapcat sale->triples xs)] (.write out (pr-str f)))
    (.append out \])))

(comment
  
  (->> "data/london-sales-2013-2014.csv"
       (io/resource)
       (load-house-sales)
       (take-nth 10)
       (write-triples-edn "data/sales-2013.edn")
       (time))
  )
