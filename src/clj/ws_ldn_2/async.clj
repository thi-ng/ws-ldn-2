(ns ws-ldn-2.async
  (:require
   [clojure.core.async :as a :refer [go go-loop <! >! timeout]]))

(defn topology1
  "Basic mult example"
  []
  (let [in      (a/chan)
        m       (a/mult in)
        t-chans (repeatedly 5 #(a/chan))]
    (doseq [[id c] (zipmap (range) t-chans)]
      (a/tap m c)
      (go-loop []
        (let [x (<! c)]
          (if-not (nil? x)
            (do
              (locking *out*
                (prn (str id " " x)))
              (recur))
            (prn :done)))))
    {:in in
     :mult m}))

(defn topology2
  "Basic mult example"
  [& fns]
  (let [in      (a/chan)
        m       (a/mult in)
        t-chans (repeatedly #(a/chan))
        o-chans (repeatedly #(a/chan))]
    (doseq [[id t o f] (partition 4 (interleave (range) t-chans o-chans fns))]
      (a/tap m t)
      (go-loop []
        (let [x (<! t)]
          (if-not (nil? x)
            (do
              (locking *out* (prn (str id " " x)))
              (>! o (f x))
              (recur))
            (prn :done)))))
    {:in   in
     :mult m
     :taps t-chans
     :outs o-chans}))


;; (zipmap (range) (take (count fns) (repeatedly ...)))
;; (partition 3 (interleave (range) (repeatedly a/chan) [f1 f2 f3]))
