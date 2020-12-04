(ns html-cljs.hooks
  "some useful hooks as examples, and to use for more hooks"
  (:require [html-cljs.html :as html]
            [html-cljs.lifecycle :as lifecycle]))

(defn use-state [initial-state]
  (let [clc html/*clc*
        hook-data (lifecycle/add-hook clc)]
    (when-not @hook-data
      (reset! hook-data {:state initial-state}))
      [(fn [] (:state @hook-data))
       (fn [swap]
         (swap! hook-data update :state swap)
         (lifecycle/refresh clc))]))

(defn use-effect [user-func]
  (let [clc html/*clc*
        hook-data (lifecycle/add-hook clc)]
    (when-not @hook-data
      (lifecycle/on-mount clc (fn [] (reset! hook-data {:cleanup (user-func)})))
      (lifecycle/on-destroy clc (fn [] (if (fn? (:cleanup @hook-data))
                                         ((:cleanup @hook-data))))))))

(defn use-interval [interval]
  (let [[get-cnt set-cnt] (use-state 0)
        [get-interval set-interval] (use-state nil)
        start-timer (fn []
                      (if (nil? (get-interval))
                        (set-interval
                          (fn [_] (js/setInterval
                                    (fn [] (set-cnt inc)) interval)))))
        stop-timer (fn [] (if (some? (get-interval))
                            (do (js/clearInterval (get-interval))
                                (set-interval (fn [_] nil)))))
        toggle (fn []
                 (if (nil? (get-interval)) (start-timer) (stop-timer)))
        _ (use-effect (fn []
                        (start-timer)
                        stop-timer))]
    [(get-cnt) toggle]))
