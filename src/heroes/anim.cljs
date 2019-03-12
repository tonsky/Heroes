(ns heroes.anim
  (:require
   [datascript.core :as ds]
   [clojure.string :as str]
   [heroes.model :as model]
   [heroes.core :as core :refer [dim pos]]))

(defn animate [*db]
  (let [db  @*db
        now (js/Date.)
        tx0 (for [e (model/entities db :aevt :sprite/anim)
                  :let [{:sprite/keys      [anim]
                         :sprite.anim/keys [frame-end]} e
                        {:anim/keys [first-frame durations-ms]} anim]
                  :when (nil? frame-end)]
              {:db/id (:db/id e)
               :sprite.anim/frame first-frame
               :sprite.anim/frame-end (core/inst-plus now (core/rand-from-range (first durations-ms)))})
        tx1 (for [e (model/entities db :aevt :sprite/anim)
                  :let [{:sprite/keys      [anim]
                         :sprite.anim/keys [frame frame-end]} e
                        {:anim/keys [first-frame durations-ms]} anim]
                  :when (and (some? frame-end)
                          (core/less? frame-end now))
                  :let [frame'    (-> frame (inc) (mod (count durations-ms)))
                        duration' (core/rand-from-range (nth durations-ms frame'))]]
              {:db/id (:db/id e)
               :sprite.anim/frame frame'
               :sprite.anim/frame-end (core/inst-plus now duration')})
        tx (concat tx0 tx1)]
    (when-not (empty? tx)
      (ds/transact! *db tx))))

(defn start! []
  (js/setInterval #(animate model/*db) 16))