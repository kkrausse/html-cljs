(ns html-cljs.demo
  (:require-macros [html-cljs.hooks :refer [mkhook]]
                   [html-cljs.html :refer [cmp]])
  (:require [html-cljs.html :as html]
            [html-cljs.hooks :refer [use-state use-effect use-interval]]
            [html-cljs.hooks :as hooks]
            [clojure.pprint :refer [pprint]]))

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


(def button (cmp [label onclick & color]
                 {:type "button"
                  :content label
                  :style {"background-color" (if-let [c (first color)] c "red")}
                  :on {"mouseup" onclick}}))

(def label (cmp [text]
                {:type "div" :content text}))


; TODO: make so we pass cmp a quoted structure so things are interpolated like
; a macro at runtime. Will basically be an analog to doing `{}` in react components
(def timer (cmp [] [[secs toggle-time] (use-interval 100)]
     {:type "div"}
     (list
       (list (cmp [text]
                  {:type "div" :content text}) "kevin's timer")
       (list label
             (str "time: " (int (/ secs 10)) "." (mod secs 10)))
       (list button "start/stop" toggle-time "grey"))))
