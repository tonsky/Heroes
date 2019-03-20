(ns heroes.model
  (:require
   [clojure.string :as str]
   [datascript.core :as ds]
   [heroes.core :as core :refer [dim pos rect Dim Pos Rect]]))

(defn entities
  ([db index c1] (map #(ds/entity db (:e %)) (ds/datoms db index c1)))
  ([db index c1 c2] (map #(ds/entity db (:e %)) (ds/datoms db index c1 c2)))
  ([db index c1 c2 c3] (map #(ds/entity db (:e %)) (ds/datoms db index c1 c2 c3))))

(defn singleton [db attr]
  (ds/entity db (:e (core/single (ds/datoms db :aevt attr)))))

(defn- tag->attr [tag]
  (cond
    (= ::identity tag)  [:db/unique :db.unique/identity]
    (= ::value tag)     [:db/unique :db.unique/value]
    (= ::component tag) [:db/isComponent true]
    (= ::index tag)     [:db/index true]
    (= ::singleton tag) [:db/unique :db.unique/identity]
    (= ::ref tag)       [:db/valueType :db.type/ref]
    (= ::many tag)      [:db/cardinality :db.cardinality/many]
    (#{::keyword ::string ::int ::instant ::boolean Dim Pos Rect} tag) nil
    (string? tag)       [:db/doc tag]
    :else               (throw (ex-info (str "Unexpected tag: " tag) {:tag tag}))))

(defn- read-schema [s]
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
     :tile
     {:coord        #{Pos ::identity}
      :rect         #{Rect}}
     :stack
     {:selected?   #{::boolean ::singleton}
      :hovered?    #{::boolean ::singleton}
      :unit-sprite #{::ref ::component}
      :count-label #{::ref ::component}
      :tile        #{::ref}
      :count       #{::int}}
     :sprite
     {:pos          #{Pos ::index}
      :anim         #{::ref}
      :mirror?      #{::boolean}
      :layers       #{::int ::many}}
     :sprite.anim
     {:frame        #{::int}
      :frame-end    #{::instant ::index}}
     :label
     {:text         #{::string}
      :pos          #{Pos}
      :align        #{::keyword "One of :left/top :center/top :right/top :left/middle :center/middle :right/middle :left/bottom :center/bottom :right/bottom"}}}))

(defonce *db (ds/create-conn schema))

(defn unhover! [stack]
  (when (:stack/hovered? stack)
    (ds/transact! *db
      (concat
        [[:db/retract (:db/id stack) :stack/hovered? true]]
        (when-not (:stack/selected? stack)
          [[:db/retract (:db/id (:stack/unit-sprite stack)) :sprite/layers 1]])))))

(defn hover! [stack]
  (let [db     @*db
        sprite (:stack/unit-sprite stack)]
    (when-some [hovered (singleton @*db :stack/hovered?)]
      (unhover! hovered))
    (ds/transact! *db
      [[:db/add (:db/id stack) :stack/hovered? true]
       [:db/add (:db/id sprite) :sprite/layers 1]])))

(defn select! [stack]
  (let [db     @*db
        sprite (:stack/unit-sprite stack)]
    (ds/transact! *db
      (concat
        (when-some [selected (singleton @*db :stack/selected?)]
          [[:db/retract (:db/id selected) :stack/selected? true]
           [:db/retract (:db/id (:stack/unit-sprite selected)) :sprite/layers 1]])
        [[:db/add (:db/id stack) :stack/selected? true]
         [:db/add (:db/id sprite) :sprite/layers 1]]))))
