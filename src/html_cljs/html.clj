(ns html-cljs.html
  (:require [clojure.walk :as w]
            [html-cljs.hooks :refer [mkhook]]))



;TODO  defrecord for components and hooks so you can use isa? to do fancy shit
; and then you won't need hooks to have a special spot in the signature!
(defmacro make-children [children]
  `(map vector
                 (map (fn [[c# ~'& ~'_]]
                        (if (symbol? c#)
                          (do (print "wow. got symbol" c#)
                              c#)
                          c#)
                        c#)
                      ~children)
                 (map rest ~children)))

(defmacro cmp
  "create component prettily. invoking the hook thing for the component itself"
  [props & args]
  (let [[hooks-or-data data-or-children or-children] args
        [hooks data children]
        (if (vector? hooks-or-data)
          [hooks-or-data data-or-children (map identity or-children)]
          [[] hooks-or-data (map identity data-or-children)])
        childs (map vector (map first children) (map #(vec (rest %)) children))]
    `(mkhook ~props ~hooks
             (html-cljs.html/map->ElementInfo
               (assoc ~data
                      :children (make-children ~children))))))
