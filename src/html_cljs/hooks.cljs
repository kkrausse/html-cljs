(ns html-cljs.hooks
  "all the hooks for this project.
  
  a hook is used with the cmp macro. A hook is a function that accepts any
  number of params that the user supplies and returns a function that accepts
  the vdom atom as its sole argument. The return value of that is the
  user facing value that is returned in the hook-calls part of the component
  creation. This is so the vdom-state atom may be passed in and used by the
  hook without the user ever knowing about the inner workings/lifecycle of the
  library."
  (:require-macros [html-cljs.hooks :refer [mkhook]])
  (:require [html-cljs.html :as html]
            [html-cljs.lifecycle :as lifecycle]))


(defn use-state [clc]
  (let [zeroth-value (symbol 'html-cljs.hooks 'rarespare)
        state-atom (atom zeroth-value)]
    (fn [initial-state]
      (if (= @state-atom zeroth-value)
        (reset! state-atom initial-state))
      [(fn [] @state-atom)
       (fn [swap]
         (swap! state-atom swap)
         (lifecycle/refresh clc))])))

(defn use-effect [clc]
  (let [initialized (atom false)
        cleanup-func (atom nil)]
    (fn []
      (fn [user-func]
        (if (not @initialized)
          (do
            (lifecycle/on-mount clc (fn [] (reset! cleanup-func (user-func))))
            (lifecycle/on-destroy clc (fn [] (if (some? @cleanup-func)
                                               (@cleanup-func))))
            (reset! initialized true)))))))

(def use-interval
  (mkhook [interval] [[get-cnt set-cnt] (use-state 0)
                      [get-interval set-interval] (use-state nil)
                      set-effect (use-effect)]
          (let [start-timer (fn []
                              (if (nil? (get-interval))
                                (set-interval
                                  (fn [_] (js/setInterval
                                            (fn [] (set-cnt inc)) interval)))))
                stop-timer (fn [] (if (some? (get-interval))
                                    (do (js/clearInterval (get-interval))
                                        (set-interval (fn [_] nil)))))
                toggle (fn []
                         (if (nil? (get-interval)) (start-timer) (stop-timer)))]
            (set-effect (fn []
                          (start-timer)
                          stop-timer))
            [(get-cnt) toggle])))
