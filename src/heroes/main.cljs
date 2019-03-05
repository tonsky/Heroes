(ns ^:figwheel-hooks heroes.main
  (:require
   [goog.dom :as gdom]
   [rum.core :as rum]
   [datascript.core :as ds]))

(defrecord Pos [x y])
(defrecord Dim [w h])

(def pos ->Pos)
(def dim ->Dim)

(defn tag->attr [tag]
  (cond
    (= ::identity tag)  [:db/unique :db.unique/identity]
    (= ::value tag)     [:db/unique :db.unique/value]
    (= ::component tag) [:db/isComponent true]
    (= ::index tag)     [:db/index true]
    (= ::ref tag)       [:db/valueType :db.type/ref]
    (= ::many tag)      [:db/cardinality :db.cardinality/many]
    (#{::keyword ::string ::int ::boolean Dim Pos} tag) nil
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
      :sprite-dim  #{Dim}}
     :anim
     {:name        #{::keyword ::identity}
      :first-frame #{::int}
      :last-frame  #{::int}
      :sheet       #{::ref}}
     :sprite
     {:pos         #{Pos}
      :sheet       #{::ref}
      :mirror?     #{::boolean}}}))

;; entities
(def initial-tx [
  {:sheet/name       :knight
   :sheet/url        "static/knight.png"
   :sheet/sprite-dim (dim 56 56)}
  {:anim/name        :knight/idle
   :anim/first-frame 0
   :anim/last-frame  1
   :anim/sheet       [:sheet/name :knight]}

  {:sheet/name       :crossbowman
   :sheet/url        "static/crossbowman.png"
   :sheet/sprite-dim (dim 56 56)}
  {:anim/name        :crossbowman/idle
   :anim/first-frame 0
   :anim/last-frame  1
   :anim/sheet       [:sheet/name :crossbowman]}

  {:sheet/name       :skeleton
   :sheet/url        "static/skeleton.png"
   :sheet/sprite-dim (dim 56 56)}
  {:anim/name        :skeleton/idle
   :anim/first-frame 0
   :anim/last-frame  1
   :anim/sheet       [:sheet/name :skeleton]}

  {:sprite/pos       (pos 45 46)
   :sprite/sheet     [:sheet/name :knight]}

  {:sprite/pos       (pos 73 46)
   :sprite/sheet     [:sheet/name :skeleton]
   :sprite/mirror?   true}
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

(defn entities
  ([db index c1] (map #(ds/entity db (:e %)) (ds/datoms db index c1)))
  ([db index c1 c2] (map #(ds/entity db (:e %)) (ds/datoms db index c1 c2)))
  ([db index c1 c2 c3] (map #(ds/entity db (:e %)) (ds/datoms db index c1 c2 c3))))

;; system
(rum/defc sprites [db]
  (for [sprite (entities db :aevt :sprite/pos) ;; components
        :let [{:sprite/keys [pos sheet mirror?]} sprite
              {:sheet/keys [sprite-dim url]} sheet]]
    [:.sprite
     {:style
      {:left   (:x pos)
       :top    (- (:y pos) (:h sprite-dim))
       :width  (:w sprite-dim)
       :height (:h sprite-dim)
       :background-image (str "url('" url "')")
       :transform (when mirror? "scale(-1,1)")}}]))

(rum/defc screen [db]
  [:.screen
   (sprites db)])

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
     (screen db)]))

(defn ^:after-load on-reload []
  (rum/mount (app) (gdom/getElement "mount")))

(defn ^:export on-load []
  (on-reload))
