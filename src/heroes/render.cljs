(ns heroes.render
  (:require
   [goog.object :as go]
   [heroes.model :as model]
   [clojure.string :as str]
   [datascript.core :as ds]
   [heroes.core :as core :refer [dim pos]]))

(defonce *images (atom {}))
(defonce *frame-time (core/clock-window 10))

(declare render!)

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
                     (if (or (> (* (:w core/screen-dim) try-scale) (:w window-dim))
                             (> (* (:h core/screen-dim) try-scale) (:h window-dim)))
                       (- try-scale step)
                       (recur (+ try-scale step))))
        window-dim (dim
                     (-> (:w window-dim) (quot scale))
                     (-> (:h window-dim) (quot scale)))]
    (assoc window-dim
      :rotate?    rotate?
      :scale      scale
      :screen-pos (pos
                    (-> (- (:w window-dim) (:w core/screen-dim)) (quot 2))
                    (-> (- (:h window-dim) (:h core/screen-dim)) (quot 2))))))

(defonce *window-dim (atom (window-dim)))

(defn render-bg! [ctx db]
  (let [bg-dim (dim 460 270)]
    (.drawImage ctx (image "static/bg.png")
      (-> (- (:w core/screen-dim) (:w bg-dim)) (quot 2))
      (-> (- (:h core/screen-dim) (:h bg-dim)) (quot 2))))
  (set! (.-strokeStyle ctx) "#fff")
  (.strokeRect ctx 0.5 0.5 (dec (:w core/screen-dim)) (dec (:h core/screen-dim))))

(defn render-tiles! [ctx db]
  (set! (.-fillStyle ctx) "rgba(255,255,255,0.3)")
  (doseq [tile (model/entities db :aevt :tile/pos)
          :let [{:tile/keys [pos coord]} tile]]
    (.fillText ctx (str (:x coord) ":" (:y coord)) (- (:x pos) 4) (+ (:y pos) 1))))

(defn render-hovers! [ctx db]
  (set! (.-fillStyle ctx) "rgba(0,0,0,0.2)")
  (doseq [stack (model/entities db :aevt :stack/tile)
          :let [{:tile/keys [pos]} (:stack/tile stack)]]
    (.fillRect ctx
      (- (:x pos) (quot (:w core/hover-dim) 2))
      (- (:y pos) 22)
      (:w core/hover-dim) (:h core/hover-dim))))

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

(defn render-labels! [ctx db]
  (doseq [label (model/entities db :aevt :label/text)
          :let [{:label/keys [text pos align]} label
                rect-w (+ 1 (.-width (.measureText ctx text)))
                rect-h 7
                halign (namespace align)
                valign (name align)
                x (case halign
                    "left"   (:x pos)
                    "center" (- (:x pos) (quot rect-w 2))
                    "right"  (- (:x pos) rect-w))
                y (case valign
                    "top"    (:y pos)
                    "middle" (- (:y pos) (quot rect-h 2))
                    "bottom" (- (:y pos) rect-h))]]
    (set! (.-fillStyle ctx) "#FCEF56")
    (.fillRect ctx x y rect-w rect-h)
    (set! (.-fillStyle ctx) "#000")
    (.fillText ctx text (+ x 1) (+ y (dec rect-h)))))

(defn render-stats! [ctx db]
  (set! (.-fillStyle ctx) "#fff")
  (let [{:keys [w h scale]} @*window-dim
        real-size   (str js/window.innerWidth "×" js/window.innerHeight)
        scaled-size (str w "×" h " at " scale "x")
        datoms      (str (count db) " datoms")
        frame-time  (str "frame " (-> (core/clock-time *frame-time) (.toFixed 1)) " ms")
        tx-time     (str "tx "    (-> (core/clock-time model/*tx-time) (.toFixed 1)) " ms")]
    (.fillText ctx (str real-size " (" scaled-size ")  " datoms "  " frame-time "  " tx-time)
      2 (- (:h core/screen-dim) 2))))

(defn render!
  ([] (render! @model/*db))
  ([db]
    (let [canvas (core/el "#canvas")
          ctx    (.getContext canvas "2d")]
      (core/clock-measure *frame-time
        (set! (.-font ctx) "5px Heroes Sans")
        (render-bg! ctx db)
        (when @core/*debug? (render-tiles! ctx db))
        (when @core/*debug? (render-hovers! ctx db))
        (render-sprites! ctx db)
        (render-labels! ctx db)
        (render-stats! ctx db)))))

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
