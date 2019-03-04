(ns ^:figwheel-hooks heroes.main
  (:require
   [goog.dom :as gdom]
   [rum.core :as rum]
   [datascript.core :as ds]))

(defn tag->attr [tag]
  (cond
    (= ::identity tag)  [:db/unique :db.unique/identity]
    (= ::value tag)     [:db/unique :db.unique/value]
    (= ::component tag) [:db/isComponent true]
    (= ::index tag)     [:db/index true]
    (= ::ref tag)       [:db/valueType :db.type/ref]
    (= ::many tag)      [:db/cardinality :db.cardinality/many]
    (#{::keyword ::string ::int ::boolean} tag) nil
    (string? tag)       [:db/doc tag]
    :else               (throw (ex-info (str "Unexpected tag: " tag) {:tag tag}))))

(defn read-schema [s]
  (into {}
    (for [[ns attrs] s
          [attr tags] attrs]
      [(keyword (name ns) (name attr))
       (into {} (map tag->attr tags))])))

(def schema
  (read-schema
    {:sheet
      {:name        #{::keyword ::identity}
       :url         #{::string "Relative to /"}
       :sprite-w    #{::int}
       :sprite-h    #{::int}}
     :anim
      {:name        #{::keyword ::identity}
       :first-frame #{::int}
       :last-frame  #{::int}
       :sheet       #{::ref}}}))

(def initial-tx [
  {:sheet/name       :knight
   :sheet/url        "static/knight.png"
   :sheet/sprite-w   56
   :sheet/sprite-h   56 }
  {:anim/name        :knight/idle
   :anim/first-frame 0
   :anim/last-frame  1
   :anim/sheet       [:sheet/name :knight]}

  {:sheet/name       :crossbowman
   :sheet/url        "static/crossbowman.png"
   :sheet/sprite-w   56
   :sheet/sprite-h   56 }
  {:anim/name        :crossbowman/idle
   :anim/first-frame 0
   :anim/last-frame  1
   :anim/sheet       [:sheet/name :crossbowman]}

  {:sheet/name       :skeleton
   :sheet/url        "static/skeleton.png"
   :sheet/sprite-w   56
   :sheet/sprite-h   56 }
  {:anim/name        :skeleton/idle
   :anim/first-frame 0
   :anim/last-frame  1
   :anim/sheet       [:sheet/name :skeleton]}
])

(def *db
  (-> (ds/empty-db schema)
      (ds/db-with initial-tx)
      (ds/conn-from-db)))

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

(rum/defc field [db]
  [:.field])

(rum/defc app
  < rum/reactive
    (on-event js/window "resize" rum/request-render)
  []
  (let [db    (rum/react *db)
        scale (scale)]
    [:.bg
     {:style
      {:width     (quot js/window.innerWidth scale)
       :height    (quot js/window.innerHeight scale)
       :transform (str "scale(" scale ")") }}
     (field db)]))

(defn ^:after-load on-reload []
  (rum/mount (app) (gdom/getElement "mount")))

(defn ^:export on-load []
  (on-reload))
