(ns ^:figwheel-hooks heroes.main
  (:require
   [goog.dom :as gdom]
   [rum.core :as rum]
   [datascript.core :as ds]
   [clojure.string :as str]))

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
    (#{::keyword ::string ::int ::instant ::boolean Dim Pos} tag) nil
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
     {:name         #{::keyword ::identity}
      :url          #{::string "Relative to /"}
      :sprite-dim   #{Dim}}
     :anim
     {:name         #{::keyword ::identity}
      :first-frame  #{::int}
      :durations-ms #{} ; [[1000 2000] [100 100] [100 100]]
      :sheet        #{::ref}}
     :sprite
     {:pos          #{Pos}
      :anim         #{::ref}
      :mirror?      #{::boolean}
      :layers       #{::int ::many}}
     :sprite.anim
     {:frame        #{::int}
      :frame-end    #{::instant}}}))

;; entities
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

  {:sprite/pos     (pos 45 46)
   :sprite/anim    [:anim/name :knight/idle]
   :sprite/layers  #{3 0 1}}
  {:sprite/pos     (pos 45 74)
   :sprite/anim    [:anim/name :knight/idle]
   :sprite/layers  #{3 0}}
  {:sprite/pos     (pos 45 102)
   :sprite/anim    [:anim/name :knight/idle]
   :sprite/layers  #{3 0}}
  {:sprite/pos     (pos 45 130)
   :sprite/anim    [:anim/name :crossbowman/idle]
   :sprite/layers  #{3 0}}
  {:sprite/pos     (pos 45 158)
   :sprite/anim    [:anim/name :crossbowman/idle]
   :sprite/layers  #{3 0}}

  {:sprite/pos     (pos 213 46)
   :sprite/anim    [:anim/name :skeleton/idle]
   :sprite/mirror? true
   :sprite/layers  #{0 2}}
  {:sprite/pos     (pos 213 74)
   :sprite/anim    [:anim/name :skeleton/idle]
   :sprite/mirror? true
   :sprite/layers  #{0 2}}
  {:sprite/pos     (pos 213 102)
   :sprite/anim    [:anim/name :skeleton/idle]
   :sprite/mirror? true
   :sprite/layers  #{0 2}}
  {:sprite/pos     (pos 213 130)
   :sprite/anim    [:anim/name :skeleton/idle]
   :sprite/mirror? true
   :sprite/layers  #{0 2}}
  {:sprite/pos     (pos 213 158)
   :sprite/anim    [:anim/name :skeleton/idle]
   :sprite/mirror? true
   :sprite/layers  #{0 2}}
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

;; Render system
(rum/defc sprites [db]
  (for [sprite (entities db :aevt :sprite/pos) ;; components
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

;; Animate system

(defn less? [a b]
  (neg? (compare a b)))

(defn rand-from-range [[from to]]
  (+ from (rand-int (- to from))))

(defn inst-plus [inst ms]
  (js/Date. (+ (.getTime inst) ms)))

(defn animate [*db]
  (let [db  @*db
        now (js/Date.)
        tx0 (for [e (entities db :aevt :sprite/anim)
                  :let [{:sprite/keys      [anim]
                         :sprite.anim/keys [frame-end]} e
                        {:anim/keys [first-frame durations-ms]} anim]
                  :when (nil? frame-end)]
              {:db/id (:db/id e)
               :sprite.anim/frame first-frame
               :sprite.anim/frame-end (inst-plus now (rand-from-range (first durations-ms)))})
        tx1 (for [e (entities db :aevt :sprite/anim)
                  :let [{:sprite/keys      [anim]
                         :sprite.anim/keys [frame frame-end]} e
                        {:anim/keys [first-frame durations-ms]} anim]
                  :when (and (some? frame-end)
                          (less? frame-end now))
                  :let [frame'    (-> frame (inc) (mod (count durations-ms)))
                        duration' (rand-from-range (nth durations-ms frame'))]]
              {:db/id (:db/id e)
               :sprite.anim/frame frame'
               :sprite.anim/frame-end (inst-plus now duration')})
        tx (concat tx0 tx1)]
    (when-not (empty? tx)
      (ds/transact! *db tx))))

(def animate-mixin
  {:did-mount
   (fn [state]
     (assoc state ::animation-timer
       (js/setInterval #(animate *db) 16)))
   :will-unmount
   (fn [state]
     (js/clearInterval (::animation-timer state))
     (dissoc state ::animation-timer))})

(rum/defc app
  < rum/reactive
    (on-event js/window "resize" rum/request-render)
    animate-mixin
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
