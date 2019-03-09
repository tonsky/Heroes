(ns heroes.render
  (:require
   [rum.core :as rum]
   [heroes.anim :as anim]
   [clojure.string :as str]
   [datascript.core :as ds]
   [heroes.model :as model]
   [heroes.core :as core :refer [dim pos]]))

(rum/defc sprites [db]
  (for [sprite (model/entities db :aevt :sprite/pos)
        :let [{:sprite/keys [pos anim mirror? layers]
               :sprite.anim/keys [frame]} sprite
              {:anim/keys [sheet]} anim
              {:sheet/keys [sprite-dim url]} sheet
              layers (->> (or layers #{0}) sort reverse vec)]]
    [:.sprite
     {:key (str (:db/id sprite))
      :style
      {:left   (:x pos)
       :top    (- (:y pos) (:h sprite-dim))
       :width  (:w sprite-dim)
       :height (:h sprite-dim)
       :background-image
       (->> (str "url('" url "')")
         (repeat (count layers))
         (str/join ", "))
       :background-position-x
       (->> (str (* frame -1 (:w sprite-dim)) "px")
         (repeat (count layers))
         (str/join ", "))
       :background-position-y
       (->> layers
         (map #(str (* % -1 (:h sprite-dim)) "px"))
         (str/join ", "))
       :transform (when mirror? "scale(-1,1)")}}]))

(rum/defc screen [db]
  [:.screen
   (sprites db)])

(defn window-dim []
  (let [w          js/window.innerWidth
        h          js/window.innerHeight
        rotate?    (< w h)
        window-dim (if rotate? (dim h w) (dim w h))
        step       0.5
        scale      (loop [try-scale (+ 1 step)]
                     (if (or (> (* 314 try-scale) (:w window-dim))
                             (> (* 176 try-scale) (:h window-dim)))
                       (- try-scale step)
                       (recur (+ try-scale step))))]
    (assoc (dim (quot (:w window-dim) scale) (quot (:h window-dim) scale))
      :rotate? rotate?
      :scale   scale)))

(rum/defcs app
  < rum/reactive
    (rum/local (window-dim) ::window-dim)
    (core/on-event js/window "resize"
      (fn [state]
        (reset! (::window-dim state) (window-dim))
        state))
    anim/mixin
  [state]
  (let [db (rum/react model/*db)
        window-dim @(::window-dim state)]
    [:.bg
     {:style
      {:width     (:w window-dim)
       :height    (:h window-dim)
       :transform (if (:rotate? window-dim)
                    (str "scale(" (:scale window-dim) ")"
                         "rotate(90deg)"
                         "translate(0,-" (:h window-dim) "px)")
                    (str "scale(" (:scale window-dim) ")")) }}
     (screen db)]))
