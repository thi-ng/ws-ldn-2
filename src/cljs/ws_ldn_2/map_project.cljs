(ns ws-ldn-2.map-project
  (:require
   [thi.ng.geom.core.vector :as v]
   [thi.ng.math.core :as m]))

(defn- lat-log
  "Helper fn to transform latitude values."
  [lat] (Math/log (Math/tan (+ (/ (m/radians lat) 2) m/QUARTER_PI))))

(defn mercator-in-rect
  "Computes mercator map projection. Take lon/lat vector, vector of
  map boundary (in degrees) and width, height of screen rect, returns
  point in screen space."
  [[lon lat] [left right top bottom] w h]
  (let [[lon left right] (mapv m/radians [lon left right])
        [lat top bottom] (mapv lat-log [lat top bottom])]
    (v/vec2
     (* w (/ (- lon left) (- right left)))
     (* h (/ (- lat top) (- bottom top))))))
