(ns heroes.core
  (:require
   [goog.object :as go])
  (:require-macros
   [heroes.core]))

(defonce *debug? (atom false))

(defrecord Pos [x y]
  IComparable
  (-compare [_ other]
    (if (== y (.-y other))
      (- x (.-x other))
      (- y (.-y other)))))

(defrecord Dim [w h])

(defrecord Rect [x y w h]
  IComparable
  (-compare [_ other]
    (if (== (+ y h) (+ (.-y other) (.-h other)))
      (- x (.-x other))
      (- (+ y h) (+ (.-y other) (.-h other))))))

(def pos ->Pos)
(def dim ->Dim)
(def rect ->Rect)

(def screen-dim (dim 314 176))
(def tile-dim (dim 28 28))
(def hover-dim (dim 24 32))

(defn less? [a b]
  (neg? (compare a b)))

(defn rand-from-range [[from to]]
  (+ from (rand-int (- to from))))

(defn inst-plus [inst ms]
  (js/Date. (+ (.getTime inst) ms)))

(defn single [xs]
  (assert (<= (count xs) 1) (str "Expected 1 or 0 element, got" xs))
  (first xs))

(defn queue [xs]
  (into (.-EMPTY PersistentQueue) xs))

(defn el [sel]
  (js/document.querySelector sel))

(defn inside? [pos box-pos box-dim]
  (and
    (>= (:x pos) (:x box-pos))
    (>= (:y pos) (:y box-pos))
    (<= (:x pos) (+ (:x box-pos) (:w box-dim)))
    (<= (:y pos) (+ (:y box-pos) (:h box-dim)))))

(defn some-map [m]
  (reduce-kv
    (fn [m k v]
      (cond
        (nil? v) (dissoc m k)
        (map? v) (if-some [v' (not-empty (some-map v))]
                   (assoc m k v')
                   (dissoc m k))
        :else m))
    m m))

(defn clock-window [size]
  (atom (queue (repeat size 0))))

(defn clock-time [*window]
  (reduce max 0 @*window))
