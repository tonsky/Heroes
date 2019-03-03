(ns ^:figwheel-hooks heroes.main
  (:require
   [goog.dom :as gdom]
   [rum.core :as rum]))

(defn scale []
  (let [ww   js/window.innerWidth
        wh   js/window.innerHeight
        step 0.5]
    (loop [try-scale (+ 1 step)]
      (if (or (> (* 314 try-scale) ww)
              (> (* 176 try-scale) wh))
        (- try-scale step)
        (recur (+ try-scale step))))))

(defn on-event [element event callback]
  {:did-mount
   (fn [state]
     (let [comp (:rum/react-component state)
           f    #(callback comp)]
       (.addEventListener element event f)
       (assoc state [::on-event event] f)))
   :will-unmount
   (fn [state]
     (.removeEventListener element event (state [::on-event event]))
     (dissoc state [::on-event event]))})

(rum/defc app
  < rum/reactive
    (on-event js/window "resize" rum/request-render)
  []
  (let [scale (scale)]
    [:.bg
     {:style
      {:width     (quot js/window.innerWidth scale)
       :height    (quot js/window.innerHeight scale)
       :transform (str "scale(" scale ")") }}
     [:.field]]))

(defn ^:after-load on-reload []
  (rum/mount (app) (gdom/getElement "mount")))

(defn ^:export on-load []
  (on-reload))
