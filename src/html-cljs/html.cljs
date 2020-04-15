(ns html-cljs.html
  "basically a mini react with a virtual dom and a diffing algorithm
  need to fix the child element problem and force id's on child elements
  because i'm just using an inefficient algorithm rn"
  (:require-macros [html-cljs.html :refer [component cmp]])
  (:require [clojure.string :as string]))

(declare render-to-state refresh steralized replace-el clear-children rerender-children)

; adds :el to the state atom and its children this
(defn domify [vdom-state-atm]
  "this creates an html element for the vdom state and its children.
  It is a call that we want to avoid if at all possible since its expensive to
  create all the dom elements

   required keys:
    :type

   optional keys:
    :on [event-name, callback] - callback will be passed the event
                                            and the element
    :style - pass dictionary and styles set as el.style[key] = value
    :content - a string to pass to the innerHTML
    :children - seq of children that will be recursively called"

  (let [state @vdom-state-atm
        node (dissoc (state :cached-render) :children)
        el (.createElement js/document (node :type))
        user-mods {:type identity
                   :input-type #(set! (.-type el) %)
                   :identifier identity ;nothing. used purely to trick the rerender alg
                   :class #(set! (.-className el) %)
                   :style #(doseq [[k v] %]
                             (aset (.-style el) k v))
                   :on (fn [[event-name f]]
                         (.addEventListener el event-name #(f %)))
                   :href #(set! (.-href el) %)
                   :content #(set! (.-innerHTML el) %)}]
    (doseq [[k v] node]
      ((user-mods k) v))
    (doseq [child (state :vdom-children)]
      (.appendChild el (domify child)))
    (swap! vdom-state-atm #(assoc % :el el))
    el))

; does the least amount of work possible to run a diff to see if we need to
; rerender something on this node (not children)
(defn ^:private create-vdom [node-func]
  (let [vdom-state (atom {})
        render (node-func vdom-state)]
    #_(prn "creating vdom el: " (render))
    (reset! vdom-state (render-to-state render))
    vdom-state))

; vdom-el is a function 2nd order function where you pass in the hooks and it
; returns a function that takes no arguments and returns the rundered object
(defn mount [html-el component]
  (clear-children html-el)
  (.appendChild
    html-el
    (domify (create-vdom component))))

; pass the mounted vdom state atom & will refresh in place
(defn refresh
  "old-vdom assumed to be domified. new-vdom assumed not to have been"
  ([vdom-atm old-vdom]
    (let [old-render (old-vdom :cached-render)
          new-vdom @vdom-atm
          new-render (new-vdom :cached-render)]
      #_(prn "comparing old: " (steralized old-render) " to new " (steralized new-render))
      ; compare and remount if necessary
      (if (= (steralized old-render) (steralized new-render))
        (doseq [[old-child new-child] (map vector (old-vdom :vdom-children) (new-vdom :vdom-children))]
          (swap! new-child #(merge @old-child %))
          (refresh new-child @old-child))
        (replace-el (old-vdom :el) (domify vdom-atm)))))
  ([vdom-atm]
    (let [vstate @vdom-atm]
      (swap! vdom-atm #(merge % (render-to-state (% :render))))
      (refresh vdom-atm vstate))))

(defn render-to-state [render]
  (let [cached-render (render)]
    {:vdom-children (for [child (cached-render :children)]
                      (create-vdom child))
     :cached-render cached-render
     :render render}))

(defn steralized [vdom]
  "sets all function nodes in map to the string 'fn'"
  (clojure.walk/postwalk
           #(if (fn? %) "fn" %)
           vdom))

(defn replace-el [old-el new-el]
  (.replaceChild (.-parentNode old-el) new-el old-el))

(defn clear-children [el]
  (if (.-firstChild el)
    (do (.removeChild el (.-firstChild el))
        (clear-children el))
    el))
