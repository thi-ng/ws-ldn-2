(ns ws-ldn-2.csvconvert
  (:require
   [thi.ng.fabric.facts.core :as ff]
   [thi.ng.strf.core :as f]
   [clojure.data.csv :as csv]
   [clojure.java.io :as io])
  (:import
   [java.util.zip GZIPInputStream GZIPOutputStream]
   [java.text SimpleDateFormat]))

(def df (SimpleDateFormat. "dd-MMM-yy"))

(defn load-csv-resource
  "Takes CSV source (path, URI or stream) and returns parsed CSV as vector of rows"
  [in]
  (let [data (-> in
                 (io/reader)
                 (csv/read-csv :separator \,))]
    (println (count data) "rows loaded")
    data))

(defn build-column-index
  "Takes a set of columns to be indexed and first row of CSV (column headers).
  Returns map of {idx col-name, ...} for all matched columns."
  [wanted-columns csv-columns]
  (->> csv-columns
       (map vector (range))
       (filter #(wanted-columns (second %)))
       (into {})))

(defn csv-row-transformer
  "Takes a column index map, set of column IDs, map of column value
  transformers, returns fn which accepts a single row vector and
  converts it into a map using keywordized column names as keys and
  their column vals. Empty column fields are elided."
  [col-idx keep-cols ftx]
  (fn [row]
    (->> row
         (map-indexed vector)
         (filter (fn [col] (keep-cols (first col))))
         (map
          (fn [[i x]]
            (let [id (keyword (col-idx i))]
              [id (if-let [f (ftx id)] (f x) x)])))
         (filter (fn [[_ x]] (if (string? x) (seq x) (not (nil? x)))))
         (into {}))))

(defn mapped-csv
  "Takes a CSV data source (path, URI or stream), a set of column
  names, field value transformer map (with keywordized column names as
  keys and single arg fns as values) and an optional row limit (default 1e9).
  Returns lazyseq of CSV result maps, one per row."
  ([src cols field-tx]
   (mapped-csv src cols field-tx 1e9))
  ([src cols field-tx limit]
   (let [rows      (take (inc limit) (load-csv-resource src))
         col-idx   (build-column-index cols (first rows))
         keep-cols (set (keys col-idx))]
     (map (csv-row-transformer col-idx keep-cols field-tx) (rest rows)))))

(defn sale->triples
  "Takes a single sales transaction record and returns seq of triples,
  using transaction UUID as common subject."
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
  "Loads CSV property sales data from given src, extracts set of
  columns and transforms column values. Returns lazyseq of maps, one
  per CSV row/record."
  [src]
  (mapped-csv
   src
   ;; CSV column fields to extract
   #{"transaction_id" "price" "date_processed" "post_code" "property_type" "borough_code"}
   ;; CSV column transformers
   {:transaction_id #(subs % 1 (dec (count %)))
    :price          #(f/parse-int % 10)
    :date_processed #(.parse df %)}))

(defn write-triples-edn
  "Takes a path or output stream, triple conversion fn and seq of data
  maps, converts each map into a seq of triples and writes all triples
  to given file."
  [path f xs]
  (with-open [out (io/writer path)]
    (.append out \[)
    (doseq [f (mapcat f xs)] (.write out (pr-str f)))
    (.append out \])))

(comment

  (->> "data/london-sales-2013-2014.csv.gz"
       (io/resource)
       (io/input-stream)
       (GZIPInputStream.)
       (load-house-sales)
       (take-nth 10)
       (write-triples-edn
        (-> "data/sales-2013.edn.gz" io/resource io/output-stream GZIPOutputStream.)
        sale->triples)
       (time))
  )
