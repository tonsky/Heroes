(ns heroes.core)

(defmacro clock-measure [*window & body]
  `(let [t0# (js/performance.now)
         _#  (do ~@body)
         dt# (- (js/performance.now) t0#)]
     (swap! ~*window #(-> % pop (conj dt#)))))