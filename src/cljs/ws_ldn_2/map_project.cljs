(ns ws-ldn-2.map-project
  (:require
   [thi.ng.geom.core.vector :as v]
   [thi.ng.math.core :as m]))

(defn lat-log
  [lat] (Math/log (Math/tan (+ (/ (m/radians lat) 2) m/QUARTER_PI))))

(defn mercator-in-rect
  [[lon lat] [left right top bottom] w h]
  (let [lon              (m/radians lon)
        left             (m/radians left)
        right            (m/radians right)
        [lat top bottom] (map lat-log [lat top bottom])]
    (v/vec2
     (* w (/ (- lon left) (- right left)))
     (* h (/ (- lat top) (- bottom top))))))
