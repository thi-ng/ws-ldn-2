(ns ws-ldn-2.utils)

(defn deep-merge
  "Merge fn to be used with `merge-with`. Recursively merges map
  values which are maps or seqs (for the latter `into` is used). If
  the RHS value has the metadata key :replace set, it is used as new
  value without merging."
  [l r]
  (cond
    (:replace (meta r))           r
    (or (sequential? l) (set? l)) (into l r)
    (map? l)                      (merge-with deep-merge l r)
    :else                         r))

(comment
  ;; example usage
  (merge-with deep-merge
              {:a 23 :b {"c" 42} :d ["e"] :f ["f"]}
              {:a2 42 :b {"c2" 66} :d ["e2"] :f ^:replace ["f2"]})
  
  ;; {:a 23
  ;;  :b {"c" 42, "c2" 66} ;; merged
  ;;  :d ["e" "e2"]        ;; merged
  ;;  :f ["f2"]            ;; replaced
  ;;  :a2 42}
  )
