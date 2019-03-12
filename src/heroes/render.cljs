(ns heroes.render
  (:require
   [goog.object :as go]
   [heroes.anim :as anim]
   [heroes.model :as model]
   [clojure.string :as str]
   [datascript.core :as ds]
   [heroes.core :as core :refer [dim pos]]))

(declare render!)

(def *window-dim (atom nil))
(def *images (atom {}))
(def screen-dim (dim 314 176))

(defn image [url]
  (or (@*images url)
    (let [img (js/Image.)]
      (go/set img "onload" (fn [_] (render!)))
      (go/set img "src" url)
      (swap! *images assoc url img)
      img)))

; (rum/defc sprites [db]
;   (for [sprite (->> (model/entities db :aevt :sprite/pos)
;                  (sort-by :sprite/pos))
;         :let [{id :db/id
;                :sprite/keys [pos anim mirror? layers]
;                :sprite.anim/keys [frame]} sprite
;               {:anim/keys [sheet]} anim
;               {:sheet/keys [sprite-dim url]} sheet
;               layers (->> (or layers #{0}) sort reverse vec)
;               stack (:stack/_unit-sprite sprite)]]
;     [:.stack
;      {:key (str id)
;       :style
;        {:left   (:x pos)
;         :top    (- (:y pos) (:h sprite-dim))
;         :width  (:w sprite-dim)
;         :height (:h sprite-dim)}}
;      [:.sprite
;       {:style
;        {:background-image
;         (->> (str "url('" url "')")
;           (repeat (count layers))
;           (str/join ", "))
;         :background-position-x
;         (->> (str (* frame -1 (:w sprite-dim)) "px")
;           (repeat (count layers))
;           (str/join ", "))
;         :background-position-y
;         (->> layers
;           (map #(str (* % -1 (:h sprite-dim)) "px"))
;           (str/join ", "))
;         :transform (when mirror? "scale(-1,1)")}}]
;      [:.sprite_hover
;       {:on-mouse-enter (fn [_] (model/hover! stack))
;        :on-mouse-leave (fn [_] (model/unhover! stack))
;        :on-click       (fn [_] (model/select! stack))}]]))

; (rum/defc screen [db]
;   [:.screen
;    (sprites db)])

(defn window-dim []
  (let [w          js/window.innerWidth
        h          js/window.innerHeight
        rotate?    (< w h)
        window-dim (if rotate? (dim h w) (dim w h))
        step       1
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

(defn render!
  ([] (render! @model/*db))
  ([db]
    (let [canvas (core/el "#canvas")
          ctx    (.getContext canvas "2d")]
      (render-bg! ctx db)
      (render-sprites! ctx db))))

(defn on-resize
  ([e] (on-resize))
  ([]
   (let [dim    (window-dim)
         _      (reset! *window-dim dim)
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

(defn start! []
  (set! js/window.onresize on-resize)
  (on-resize))
