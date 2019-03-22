(ns heroes.input
  (:require
   [goog.object :as go]
   [heroes.model :as model]
   [clojure.string :as str]
   [datascript.core :as ds]
   [heroes.render :as render]
   [heroes.core :as core :refer [dim pos]]))

(def *hover-sprite-eid (atom nil))

(defn sprite-eid [event]
  (let [db         @model/*db
        screen-pos (render/window->screen (pos (.-clientX event) (.-clientY event)))
        datoms     (ds/index-range db :sprite/pos
                     (pos 59  (- (:y screen-pos) (:h core/hover-dim)))
                     (pos 255 (+ (:y screen-pos) (:h core/hover-dim))))]
    (some
      (fn [datom]
        (let [sprite-pos (:v datom)
              hover-pos  (pos (- (:x sprite-pos) (quot (:w core/hover-dim) 2))
                              (- (:y sprite-pos) (:h core/hover-dim)))]
          (when (core/inside? screen-pos hover-pos core/hover-dim)
            (:e datom))))
      (some-> datoms rseq))))

(defn on-mouse-move [e]
  (reset! *hover-sprite-eid (sprite-eid e)))

(defn on-mouse-leave [sprite-eid]
  (let [db     @model/*db
        sprite (ds/entity db sprite-eid)
        stack  (:stack/_unit-sprite sprite)]
    (model/unhover! stack)))

(defn on-mouse-enter [sprite-eid]
  (let [db     @model/*db
        sprite (ds/entity db sprite-eid)
        stack  (:stack/_unit-sprite sprite)]
    (model/hover! stack)))

(defn on-mouse-click [e]
  (when-some [sprite-eid (sprite-eid e)]
    (let [db         @model/*db
          sprite     (ds/entity db sprite-eid)
          stack      (:stack/_unit-sprite sprite)]
      (model/select! stack))))

(defn on-key-down [e]
  #_(println "KEY" (.-key e) "code" (.-code e))
  (case (.-code e)
    "KeyD" (swap! core/*debug? not)
    nil))

(defn reload! []
  (let [canvas (core/el "#canvas")]
  (set! (.-onmousemove canvas) on-mouse-move)
  (add-watch *hover-sprite-eid ::mouseleave
    (fn [_ _ old new]
      (when (and (some? old) (not= old new))
        (on-mouse-leave old))))
  (add-watch *hover-sprite-eid ::mouseenter
    (fn [_ _ old new]
      (when (and (some? new) (not= old new))
        (on-mouse-enter new))))
  (set! (.-onclick canvas) on-mouse-click)
  (set! js/window.onkeydown on-key-down)))
