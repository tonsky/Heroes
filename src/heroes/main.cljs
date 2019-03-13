(ns ^:figwheel-hooks heroes.main
  (:require
   [heroes.anim :as anim]
   [heroes.input :as input]
   [heroes.model :as model]
   [clojure.string :as str]
   [datascript.core :as ds]
   [heroes.render :as render]
   [heroes.core :as core :refer [dim pos]]))

(def initial-tx [
  {:sheet/name        :knight
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
   :anim/sheet        [:sheet/name :zombie]}

  {:stack/unit-sprite  
   {:sprite/pos     (pos 73 74)
    :sprite/anim    [:anim/name :knight/idle]
    :sprite.anim/frame 0
    :sprite.anim/frame-end (js/Date.)
    :sprite/layers  #{3 0}}}
  {:stack/selected? true
   :stack/unit-sprite
   {:sprite/pos     (pos 73 46)
    :sprite/anim    [:anim/name :knight/idle]
    :sprite.anim/frame 0
    :sprite.anim/frame-end (js/Date.)
    :sprite/layers  #{3 0 1}}}
  {:stack/unit-sprite  
   {:sprite/pos     (pos 73 102)
    :sprite/anim    [:anim/name :knight/idle]
    :sprite.anim/frame 0
    :sprite.anim/frame-end (js/Date.)
    :sprite/layers  #{3 0}}}
  {:stack/unit-sprite  
   {:sprite/pos     (pos 73 130)
    :sprite/anim    [:anim/name :crossbowman/idle]
    :sprite.anim/frame 0
    :sprite.anim/frame-end (js/Date.)
    :sprite/layers  #{3 0}}}
  {:stack/unit-sprite  
   {:sprite/pos     (pos 73 158)
    :sprite/anim    [:anim/name :crossbowman/idle]
    :sprite.anim/frame 0
    :sprite.anim/frame-end (js/Date.)
    :sprite/layers  #{3 0}}}

  {:stack/unit-sprite  
   {:sprite/pos     (pos 241 46)
    :sprite/anim    [:anim/name :zombie/idle]
    :sprite.anim/frame 0
    :sprite.anim/frame-end (js/Date.)
    :sprite/mirror? true
    :sprite/layers  #{0 2}}}
  {:stack/unit-sprite  
   {:sprite/pos     (pos 241 74)
    :sprite/anim    [:anim/name :zombie/idle]
    :sprite.anim/frame 0
    :sprite.anim/frame-end (js/Date.)
    :sprite/mirror? true
    :sprite/layers  #{0 3}}}
  {:stack/unit-sprite  
   {:sprite/pos     (pos 213 102)
    :sprite/anim    [:anim/name :zombie/idle]
    :sprite.anim/frame 0
    :sprite.anim/frame-end (js/Date.)
    :sprite/mirror? true
    :sprite/layers  #{0}}}
  {:stack/unit-sprite  
   {:sprite/pos     (pos 241 130)
    :sprite/anim    [:anim/name :skeleton/idle]
    :sprite.anim/frame 0
    :sprite.anim/frame-end (js/Date.)
    :sprite/mirror? true
    :sprite/layers  #{0 2}}}
  {:stack/unit-sprite  
   {:sprite/pos     (pos 241 158)
    :sprite/anim    [:anim/name :skeleton/idle]
    :sprite.anim/frame 0
    :sprite.anim/frame-end (js/Date.)
    :sprite/mirror? true
    :sprite/layers  #{0 2}}}
])

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
