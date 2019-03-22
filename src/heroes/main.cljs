(ns ^:figwheel-hooks heroes.main
  (:require
   [heroes.anim :as anim]
   [heroes.input :as input]
   [heroes.model :as model]
   [clojure.string :as str]
   [datascript.core :as ds]
   [heroes.render :as render]
   [heroes.core :as core :refer [dim pos rect]]))

(def sheets
  [{:sheet/name        :knight
    :sheet/url         "static/knight.png"
    :sheet/sprite-dim  (dim 56 56)}
   {:anim/name         :knight/idle
    :anim/first-frame  0
    :anim/durations-ms [[500 1000] [300 400]]
    :anim/sheet        [:sheet/name :knight]}

   {:sheet/name        :crossbowman
    :sheet/url         "static/crossbowman.png"
    :sheet/sprite-dim  (dim 56 56)}
   {:anim/name         :crossbowman/idle
    :anim/first-frame  0
    :anim/durations-ms [[500 1000] [200 200] [200 200] [200 200]]
    :anim/sheet        [:sheet/name :crossbowman]}

   {:sheet/name        :skeleton
    :sheet/url         "static/skeleton.png"
    :sheet/sprite-dim  (dim 56 56)}
   {:anim/name         :skeleton/idle
    :anim/first-frame  0
    :anim/durations-ms [[200 500] [200 200] [200 200] [200 200]]
    :anim/sheet        [:sheet/name :skeleton]}

   {:sheet/name        :zombie
    :sheet/url         "static/zombie.png"
    :sheet/sprite-dim  (dim 56 56)}
   {:anim/name         :zombie/idle
    :anim/first-frame  0
    :anim/durations-ms [[200 500] [200 200] [200 200] [200 200]]
    :anim/sheet        [:sheet/name :zombie]}])
    
(def tiles
  (for [y (range 0 5)
        x (range 0 7)]
    {:tile/coord (pos x y)
     :tile/pos   (pos
                   (-> (* x 28) (+ 73))
                   (-> (* y 28) (+ 32)))}))

(defn place-stack [db {:keys [coord count unit selected? player]}]
  (let [tile (ds/entity db [:tile/coord coord])
        {tile-pos :tile/pos} tile]
    [(core/some-map
       {:stack/selected? selected?
        :stack/tile      (:db/id tile)
        :stack/count     count
        :stack/count-label
        (if (= 0 player)
          {:label/pos   (pos
                          (- (:x tile-pos) 10)
                          (+ (:y tile-pos) 4))
           :label/align :right/bottom
           :label/text  (str count)}
          {:label/pos   (pos
                          (+ (:x tile-pos) 10)
                          (+ (:y tile-pos) 4))
           :label/align :left/bottom
           :label/text  (str count)})
        :stack/unit-sprite
        {:sprite/pos            (pos (:x tile-pos) (+ (:y tile-pos) 14))
         :sprite/mirror?        (= player 1)
         :sprite/anim           [:anim/name (keyword (name unit) "idle")]
         :sprite.anim/frame     0
         :sprite.anim/frame-end (js/Date.)
         :sprite/layers         (remove nil?
                                  [0
                                   (when selected? 1)
                                   (when player (+ player 2))])}})]))

(def stacks
  [[:db.fn/call place-stack
    {:coord  (pos 0 1)
     :count  2
     :unit   :knight
     :player 0
     :selected? true}]
   [:db.fn/call place-stack
    {:coord  (pos 0 0)
     :count  70
     :unit   :knight
     :player 0}]
   [:db.fn/call place-stack
    {:coord  (pos 0 2)
     :count  222
     :unit   :knight
     :player 0}]
   [:db.fn/call place-stack
    {:coord  (pos 0 3)
     :count  1
     :unit   :crossbowman
     :player 0}]
   [:db.fn/call place-stack
    {:coord  (pos 0 4)
     :count  9
     :unit   :crossbowman
     :player 0}]

   [:db.fn/call place-stack
    {:coord  (pos 6 0)
     :count  5
     :unit   :zombie
     :player 1}]
   [:db.fn/call place-stack
    {:coord  (pos 6 1)
     :count  347
     :unit   :zombie
     :player 1}]
   [:db.fn/call place-stack
    {:coord  (pos 5 2)
     :count  98
     :unit   :zombie
     :player 1}]
   [:db.fn/call place-stack
    {:coord  (pos 6 3)
     :count  1234
     :unit   :skeleton
     :player 1}]
   [:db.fn/call place-stack
    {:coord  (pos 6 4)
     :count  10000
     :unit   :skeleton
     :player 1}]])

(def initial-tx
  (concat
    sheets
    tiles
    stacks))

(defn ^:after-load on-reload []
  (render/reload!)
  (input/reload!))

(defn ^:export on-load []
  (reset! model/*db (-> (ds/empty-db model/schema)
                      (ds/db-with initial-tx)))
  (render/reload!)
  (input/reload!)
  (anim/start!)
  (ds/listen! model/*db ::rerender
    (fn [report]
      (js/requestAnimationFrame
        #(render/render! (:db-after report))))))
