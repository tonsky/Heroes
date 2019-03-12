(ns heroes.core
  (:require
   [goog.object :as go]))

(defrecord Pos [x y]
  IComparable
  (-compare [_ other]
    (if (== y (.-y other))
      (- x (.-x other))
      (- y (.-y other)))))

(defrecord Dim [w h])

(def pos ->Pos)
(def dim ->Dim)

(defn less? [a b]
  (neg? (compare a b)))

(defn rand-from-range [[from to]]
  (+ from (rand-int (- to from))))

(defn inst-plus [inst ms]
  (js/Date. (+ (.getTime inst) ms)))

(defn single [xs]
  (assert (<= (count xs) 1) (str "Expected 1 or 0 element, got" xs))
  (first xs))

(defn el [sel]
  (js/document.querySelector sel))

(defn inside? [pos box-pos box-dim]
  (and
    (>= (:x pos) (:x box-pos))
    (>= (:y pos) (:y box-pos))
    (<= (:x pos) (+ (:x box-pos) (:w box-dim)))
    (<= (:y pos) (+ (:y box-pos) (:h box-dim)))))