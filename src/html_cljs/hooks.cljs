(ns html-cljs.hooks
  "all the hooks for this project.
  
  a hook is used with the cmp macro. A hook is a function that accepts any
  number of params that the user supplies and returns a function that accepts
  the vdom atom as its sole argument. The return value of that is the
  user facing value that is returned in the hook-calls part of the component
  creation. This is so the vdom-state atom may be passed in and used by the
  hook without the user ever knowing about the inner workings/lifecycle of the
  library."
  (:require [html-cljs.html :as html]))

(defn use-state [init]
  (let [state (atom init)]
    (fn [vdom-state]
      [@state
       (fn [swap-state]
         (swap! state swap-state)
         (html/refresh vdom-state))])))

(defn use-dom-el []
  (fn [vdom-state]
    #(@vdom-state :el)))
