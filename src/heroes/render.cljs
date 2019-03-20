(ns heroes.render
  (:require
   [goog.object :as go]
   [heroes.model :as model]
   [clojure.string :as str]
   [datascript.core :as ds]
   [heroes.core :as core :refer [dim pos]]))

(declare render!)

(def *window-dim (atom nil))
(def *images (atom {}))
(def screen-dim (dim 314 176))
(def *frame-times-ms (atom (into (.-EMPTY PersistentQueue) (repeat 60 0))))

(defn image [url]
  (or (@*images url)
    (let [img (js/Image.)]
      (go/set img "onload" (fn [_] (render!)))
      (go/set img "src" url)
      (swap! *images assoc url img)
      img)))

(defn window-dim []
  (let [w          js/window.innerWidth
        h          js/window.innerHeight
        rotate?    (< w h)
        window-dim (if rotate? (dim h w) (dim w h))
        step       1 ;; TODO
        scale      (loop [try-scale (+ 1 step)]
                     (if (or (> (* (:w screen-dim) try-scale) (:w window-dim))
                             (> (* (:h screen-dim) try-scale) (:h window-dim)))
                       (- try-scale step)
                       (recur (+ try-scale step))))
        window-dim (dim
                     (-> (:w window-dim) (quot scale))
                     (-> (:h window-dim) (quot scale)))]
    (assoc window-dim
      :rotate?    rotate?
      :scale      scale
      :screen-pos (pos
                    (-> (- (:w window-dim) (:w screen-dim)) (quot 2))
                    (-> (- (:h window-dim) (:h screen-dim)) (quot 2))))))

(defn render-bg! [ctx db]
  (let [bg-dim (dim 460 270)]
    (.drawImage ctx (image "static/bg.png")
      (-> (- (:w screen-dim) (:w bg-dim)) (quot 2))
      (-> (- (:h screen-dim) (:h bg-dim)) (quot 2))))
  (set! (.-strokeStyle ctx) "#fff")
  (.strokeRect ctx 0.5 0.5 (dec (:w screen-dim)) (dec (:h screen-dim))))

(defn render-sprites! [ctx db]
  (doseq [sprite (->> (model/entities db :aevt :sprite/pos)
                   (sort-by :sprite/pos))
          :let [{:sprite/keys [pos anim mirror? layers]
                 :sprite.anim/keys [frame]}    sprite
                {:anim/keys [sheet]}           anim
                {:sheet/keys [sprite-dim url]} sheet
                img                            (image url)]]
    (.save ctx)
    (.translate ctx (:x pos) (:y pos))
    (when mirror?
      (.scale ctx -1 1))
    (doseq [layer (->> (or layers #{0}) sort)]
      (.drawImage ctx img
        (* frame (:w sprite-dim))
        (* layer (:h sprite-dim))
        (:w sprite-dim)
        (:h sprite-dim)
        (- (quot (:w sprite-dim) 2))
        (- (:h sprite-dim))
        (:w sprite-dim)
        (:h sprite-dim)))
    (.restore ctx)))

(defn render-stats! [ctx db]
  (set! (.-fillStyle ctx) "#fff")
  (set! (.-font ctx) "5px Heroes Sans")
  (let [{:keys [w h scale]} @*window-dim
        real-size   (str js/window.innerWidth "×" js/window.innerHeight)
        scaled-size (str w "×" h " at " scale "x")
        datoms      (str (count db) " datoms")
        frame-time  (str (-> (reduce max 0 @*frame-times-ms) (.toFixed 1)) " ms")]
    (.fillText ctx (str real-size " (" scaled-size ")  " datoms "  " frame-time)
      2 (- (:h screen-dim) 2))))

(defn render!
  ([] (render! @model/*db))
  ([db]
    (let [canvas (core/el "#canvas")
          ctx    (.getContext canvas "2d")
          t0     (js/performance.now)
          _      (render-bg! ctx db)
          _      (render-sprites! ctx db)
          _      (render-stats! ctx db)
          dt     (- (js/performance.now) t0)]
      (swap! *frame-times-ms #(-> % (pop) (conj dt))))))

(defn on-resize
  ([e] (on-resize))
  ([]
   (let [dim    (reset! *window-dim (window-dim))
         {:keys [scale w h rotate? screen-pos]} dim
         canvas (core/el "#canvas")
         ctx    (.getContext canvas "2d")
         style  (.-style canvas)]
     (set! (.-width canvas)  w)
     (set! (.-height canvas) h)
     (set! (.-width style)  (str (* scale w) "px"))
     (set! (.-height style) (str (* scale h) "px"))

     (set! (.-transformOrigin style) (str (* scale h 0.5) "px " (* scale h 0.5) "px"))
     (set! (.-transform style) (if rotate? "rotate(90deg)" ""))

     (.translate ctx (:x screen-pos) (:y screen-pos)))
   (render!)))

(defn reload! []
  (set! js/window.onresize on-resize)
  (on-resize))

(defn window->screen [wpos]
  (let [{:keys [rotate? scale screen-pos]} @*window-dim
        wpos' (if rotate?
                (pos (:y wpos) (- js/window.innerWidth (:x wpos)))
                wpos)]
    (pos
      (-> (:x wpos') (quot scale) (- (:x screen-pos)))
      (-> (:y wpos') (quot scale) (- (:y screen-pos))))))
