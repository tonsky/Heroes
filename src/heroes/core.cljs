(ns heroes.core
  (:require
   [rum.core :as rum]))

(defrecord Pos [x y]
  IComparable
  (-compare [_ other]
    (if (== y (.-y other))
      (- x (.-x other))
      (- y (.-y other)))))

(defrecord Dim [w h])

(def pos ->Pos)
(def dim ->Dim)

(defn less? [a b]
  (neg? (compare a b)))

(defn rand-from-range [[from to]]
  (+ from (rand-int (- to from))))

(defn inst-plus [inst ms]
  (js/Date. (+ (.getTime inst) ms)))

(defn on-event [element event callback]
  {:did-mount
   (fn [state]
     (let [comp (:rum/react-component state)
           f    #(vswap! (rum/state comp) callback)]
       (.addEventListener element event f)
       (assoc state [::on-event event] f)))
   :will-unmount
   (fn [state]
     (.removeEventListener element event (state [::on-event event]))
     (dissoc state [::on-event event]))})

(defn single [xs]
  (assert (<= (count xs) 1) (str "Expected 1 or 0 element, got" xs))
  (first xs))