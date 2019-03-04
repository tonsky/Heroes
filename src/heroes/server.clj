(ns heroes.server
  (:require
   [clojure.string :as str]
   [ring.util.response :as response]))

(defn static [{:keys [uri] :as req}]
  (when (str/starts-with? uri "/static")
    (response/file-response uri {:root "docs"})))