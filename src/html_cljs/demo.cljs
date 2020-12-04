(ns html-cljs.demo
  (:require [html-cljs.html :as html]
            [html-cljs.hooks :refer [use-state use-effect use-interval]]
            [html-cljs.hooks :as hooks]
            [clojure.pprint :refer [pprint]]
            [clojure.walk :as walk]))

(declare
  timer)

(defonce mounted (atom nil))

(defn mount-root [component]
  (swap! mounted
         (fn [old-vdom-node]
           (if (some? old-vdom-node)
             (html/destroy-node old-vdom-node))
           (html/mount
             (.getElementById js/document "app")
             component))))

(defn ^:export init []

  (mount-root timer)
  
  )

(defn timer []
  (let [[get-state set-state] (use-state 0)
        [secs toggle] (use-interval 10)
        click (fn [] (set-state inc))]
  (html/elem {:type "div"}
    (list html/elem {:type "div" :content (str "count: " (get-state))})
    (list html/elem {:type "div"
                 :content (str "time: " (int (/ secs 100)) "." (mod secs 100))})
    (list html/elem {:type "button"
             :content "start/stop"
             :style {"background-color" "cyan"}
             :on {"mouseup" (fn [] (toggle) (set-state inc))}}))))
