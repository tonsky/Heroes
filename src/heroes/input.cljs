(ns heroes.input
  (:require
   [goog.object :as go]
   [heroes.model :as model]
   [clojure.string :as str]
   [datascript.core :as ds]
   [heroes.render :as render]
   [heroes.core :as core :refer [dim pos]]))

(def *hover-stack-eid (atom nil))

(defn stack-eid [event]
  (let [db         @model/*db
        screen-pos (render/window->screen (pos (.-clientX event) (.-clientY event)))
        datoms     (ds/index-range db :tile/pos
                     (pos 0                    (- (:y screen-pos) (:h core/hover-dim)))
                     (pos (:w core/screen-dim) (+ (:y screen-pos) (:h core/hover-dim))))]
    (some
      (fn [datom]
        (let [tile-pos  (:v datom)
              hover-pos (pos
                          (- (:x tile-pos) (quot (:w core/hover-dim) 2))
                          (- (:y tile-pos) 22))]
          (when (core/inside? screen-pos hover-pos core/hover-dim)
            (let [tile  (ds/entity db (:e datom))
                  stack (core/single (:stack/_tile tile))]
              (:db/id stack)))))
      (some-> datoms rseq))))

(defn on-mouse-move [e]
  (reset! *hover-stack-eid (stack-eid e)))

(defn on-mouse-leave [stack-eid]
  (model/unhover! (ds/entity @model/*db stack-eid)))

(defn on-mouse-enter [stack-eid]
  (model/hover! (ds/entity @model/*db stack-eid)))

(defn on-mouse-click [e]
  (when-some [stack-eid (stack-eid e)]
    (model/select! (ds/entity @model/*db stack-eid))))

(defn on-key-down [e]
  #_(println "KEY" (.-key e) "code" (.-code e))
  (case (.-code e)
    "KeyD" (swap! core/*debug? not)
    nil))

(defn reload! []
  (let [canvas (core/el "#canvas")]
  (set! (.-onmousemove canvas) on-mouse-move)
  (add-watch *hover-stack-eid ::mouseleave
    (fn [_ _ old new]
      (when (and (some? old) (not= old new))
        (on-mouse-leave old))))
  (add-watch *hover-stack-eid ::mouseenter
    (fn [_ _ old new]
      (when (and (some? new) (not= old new))
        (on-mouse-enter new))))
  (set! (.-onclick canvas) on-mouse-click)
  (set! js/window.onkeydown on-key-down)))
