(ns ^:figwheel-hooks heroes.main
  (:require
   [heroes.anim :as anim]
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
   {:sprite/pos     (pos 45 74)
    :sprite/anim    [:anim/name :knight/idle]
    :sprite/layers  #{3 0}}}
  {:stack/selected? true
   :stack/unit-sprite
   {:sprite/pos     (pos 45 46)
    :sprite/anim    [:anim/name :knight/idle]
    :sprite/layers  #{3 0 1}}}
  {:stack/unit-sprite  
   {:sprite/pos     (pos 45 102)
    :sprite/anim    [:anim/name :knight/idle]
    :sprite/layers  #{3 0}}}
  {:stack/unit-sprite  
   {:sprite/pos     (pos 45 130)
    :sprite/anim    [:anim/name :crossbowman/idle]
    :sprite/layers  #{3 0}}}
  {:stack/unit-sprite  
   {:sprite/pos     (pos 45 158)
    :sprite/anim    [:anim/name :crossbowman/idle]
    :sprite/layers  #{3 0}}}

  {:stack/unit-sprite  
   {:sprite/pos     (pos 213 46)
    :sprite/anim    [:anim/name :zombie/idle]
    :sprite/mirror? true
    :sprite/layers  #{0 2}}}
  {:stack/unit-sprite  
   {:sprite/pos     (pos 213 74)
    :sprite/anim    [:anim/name :zombie/idle]
    :sprite/mirror? true
    :sprite/layers  #{0 3}}}
  {:stack/unit-sprite  
   {:sprite/pos     (pos 185 102)
    :sprite/anim    [:anim/name :zombie/idle]
    :sprite/mirror? true
    :sprite/layers  #{0}}}
  {:stack/unit-sprite  
   {:sprite/pos     (pos 213 130)
    :sprite/anim    [:anim/name :skeleton/idle]
    :sprite/mirror? true
    :sprite/layers  #{0 2}}}
  {:stack/unit-sprite  
   {:sprite/pos     (pos 213 158)
    :sprite/anim    [:anim/name :skeleton/idle]
    :sprite/mirror? true
    :sprite/layers  #{0 2}}}
])

(defn ^:after-load on-reload []
  (render/on-resize))

(defn ^:export on-load []
  (reset! model/*db (-> (ds/empty-db model/schema)
                      (ds/db-with initial-tx)))
  (render/start!)
  (anim/start!)
  (ds/listen! model/*db ::rerender (fn [report] (render/render! (:db-after report)))))
