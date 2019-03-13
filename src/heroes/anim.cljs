(ns heroes.anim
  (:require
   [datascript.core :as ds]
   [clojure.string :as str]
   [heroes.model :as model]
   [heroes.core :as core :refer [dim pos]]))

(defn animate [*db]
  (let [db     @*db
        now    (js/Date.)
        datoms (ds/index-range db :sprite.anim/frame-end nil now)
        tx     (for [datom datoms
                     :let [sprite    (ds/entity db (:e datom))
                           frame-end (:v datoms)
                           {:sprite/keys      [anim]
                            :sprite.anim/keys [frame]} sprite
                           {:anim/keys [first-frame durations-ms]} anim
                           frame'    (-> frame (inc) (mod (count durations-ms)))
                           duration' (core/rand-from-range (nth durations-ms frame'))]]
                 {:db/id (:e datom)
                  :sprite.anim/frame frame'
                  :sprite.anim/frame-end (core/inst-plus now duration')})]
    (when-not (empty? tx)
      (ds/transact! *db tx))))

(defn start! []
  (js/setInterval #(animate model/*db)) 100)