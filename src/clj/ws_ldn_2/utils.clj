(ns ws-ldn-2.utils)

(defn deep-merge
  [l r]
  (cond
    (:replace (meta r))           r
    (or (sequential? l) (set? l)) (into l r)
    (map? l)                      (merge-with deep-merge l r)
    :else                         r))

(comment
  (merge-with deep-merge
              {:a 23 :b {"c" 42} :d ["e"] :f ["f"]}
              {:a2 42 :b {"c2" 66} :d ["e2"] :f ^:replace ["f2"]})
  
  ;; {:a 23, :b {"c" 42, "c2" 66}, :d ["e" "e2"], :f ["f2"], :a2 42}
  )
