(ns html-cljs.html
  (:require [html-cljs.lifecycle :as lifecycle]
            [html-cljs.nouns :refer [bind-lifecycle]]
            [clojure.walk :as walk]))

(declare mount
         steralized
         add-callbacks
         rm-callbacks
         set-style
         elem
         component->VDomNode
         extendzip)

(defprotocol InternalLifecycle
  (destroy-node [_] "user-defined ondestroy callbacks")
  (mount-node [_] "user-defined onmount callbacks")
  (replace-html [_ elem-info] "recreate the dom element using supplied elem info")
  (add-child [_ vdom-node] "append this child element to end of children list")
  (remove-child [_ vdom-node])
  (replace-child [_ old-vdom-node new-vdom-node]))

(defprotocol ElementWrapper
  (create-html-elem [_])
  (replace-wrapper [_ new-wrapper that])
  (destroy-elem [_ html-elem] "cleans up callbacks and removes the dom node"))

(defrecord ElementInfo
  [type id class elem-props style on href content children]
  ElementWrapper
  (create-html-elem [this]
    "creates element for component and NOT its children.

    required keys:
    :type

    optional keys:
    :on {event-name callback...} - callback will be passed the event
    :style - pass dictionary and styles set as el.style[key] = value
    :content - a string to pass to the innerHTML
    :children - seq of tuples (vec2's) that have component and props"

    (let [node (dissoc this :children)
          el (.createElement js/document (node :type))
          user-mods {:type identity
                     :id identity ;nothing. used to trick the rerender alg
                     :class #(set! (.-className el) %)
                     :elem-props #(doseq [[k v] %]
                                    (aset el k v))
                     :style #(doseq [[k v] %]
                               (aset (.-style el) k v))
                     :on #(add-callbacks el %)
                     :href #(set! (.-href el) %)
                     :content #(set! (.-innerHTML el) %)}]
      (doseq [[k v] node]
        (if (some? v)
          ((user-mods k) v)))
      el))
  (replace-wrapper [this html-elem that]
    (rm-callbacks html-elem on)
    (add-callbacks html-elem (:on that))
    (set-style html-elem (:style that)))
  (destroy-elem [_ html-elem]
    (rm-callbacks html-elem on)
    (.remove html-elem)))

(defrecord VDomNode [props-atm hooked-component-atm
                     child-nodes-atm html-elem-atm cached-render-atm
                     onmounts-atm ondestroys-atm hook-data]
  InternalLifecycle
  (destroy-node [_]
    (doseq [child @child-nodes-atm]
      (destroy-node child))
    (doseq [f @ondestroys-atm]
      (f)))

  (mount-node [_]
    (doseq [f @onmounts-atm]
      (f)))

  (replace-html [_ elem-info]
    (let [new-elem (create-html-elem elem-info)
          old-elem @html-elem-atm
          old-parent (.-parentNode old-elem)]
      (doseq [child-node @child-nodes-atm]
        (.appendChild new-elem @(:html-elem-atm child-node)))
      
      (.replaceChild old-parent new-elem old-elem)
      (destroy-elem @cached-render-atm old-elem)
      (reset! html-elem-atm new-elem)))

  (add-child [_ vdom-node]
    (swap! child-nodes-atm #(conj % vdom-node))
    (.appendChild @html-elem-atm @(:html-elem-atm vdom-node)))

  (remove-child [_ vdom-node]
    (swap! child-nodes-atm (fn [nodes] (remove #(= % vdom-node) nodes)))
    (.removeChild @html-elem-atm @(:html-elem-atm vdom-node))
    (destroy-node vdom-node))

  (replace-child [_ old-vdom-node new-vdom-node]
    (swap! child-nodes-atm (fn [nodes]
                             (walk/prewalk
                               #(if (= % old-vdom-node)
                                  new-vdom-node
                                  %) nodes)))
    (.replaceChild @html-elem-atm @(:html-elem-atm new-vdom-node) @(:html-elem-atm old-vdom-node))
    (destroy-node old-vdom-node))


  lifecycle/LifecycleHooks
  (rerender [this new-props]
    (let [new-elem-info (@hooked-component-atm new-props)]
      (if (= (steralized new-elem-info) (steralized @cached-render-atm))
        (replace-wrapper @cached-render-atm @html-elem-atm new-elem-info)
        (replace-html this new-elem-info))
      (doseq
        [[[child-node old-component old-props]
          [new-component new-props]]
         (extendzip
           (extendzip @child-nodes-atm
                      (map first (:children @cached-render-atm))
                      (map second (:children @cached-render-atm)))
           (:children new-elem-info))]
        (cond
          (nil? child-node) (add-child this
                                       (component->VDomNode new-component new-props))
          (nil? new-component) (remove-child this child-node)
          (and (not= (hash old-component) (hash new-component))
               (not= old-props new-props)) (replace-child
                                             this
                                             child-node
                                             (component->VDomNode
                                               new-component
                                               new-props))
          (not= old-props new-props) (lifecycle/rerender child-node new-props)
          :else nil))
      (reset! cached-render-atm new-elem-info)
      (reset! props-atm new-props)
      this))
  (getprops [this] @props-atm)
  (add-hook [this]
    (let [hook-num (:current @hook-data)]
      (swap! hook-data update :current inc); update for next hook
      (if (< (:current @hook-data) (count (:user-data @hook-data)))
        (-> @hook-data :user-data (get hook-num))
        (do
          (swap! hook-data update :user-data (fn [old] (conj old (atom nil))))
          (-> @hook-data :user-data (get hook-num))))))
  (on-mount [_ f] (swap! onmounts-atm #(conj % f)))
  (on-destroy [_ f] (swap! ondestroys-atm #(conj % f))))

(defn elem [elem-data & children]
  "helper to create elements"
  (map->ElementInfo
    (assoc elem-data
           :children (map (fn [[h & t]] [h t]) children))))

; component lifecycle. Used in hooks when rendering.
(def ^:dynamic *clc* nil)

(defn component->VDomNode
  "create (and bind to html-dom) a full VDomNode from this component"
  ([component props]
   (let [node (map->VDomNode {:props-atm (atom props)
                              :hooked-component-atm (atom nil)
                              :child-nodes-atm (atom [])
                              :html-elem-atm (atom nil)
                              :cached-render-atm (atom nil)
                              :onmounts-atm (atom [])
                              :ondestroys-atm (atom [])
                              :hook-data (atom {:current 0
                                                :user-data []})})
         hooked-component (fn [props]
                            (swap! (:hook-data node) assoc :current 0)
                            (binding [*clc* node]
                                      (apply component props)))
         elem-info (hooked-component props)]
     (reset! (:hooked-component-atm node) hooked-component)
     (reset! (:html-elem-atm node) (create-html-elem elem-info))
     (reset! (:cached-render-atm node) elem-info)
     (doseq [[child-component props] (:children elem-info)]
       (add-child node
                  (component->VDomNode child-component props)))
     (mount-node node)
     node)))

(defn extendzip
  "zips arbitrary number of seqs filling in nils if some are shorter"
  [& ls]
  (let [maxlen (apply max (map count ls))]
    (apply (partial map vector)
           (map (fn [l]
                  (concat l (map #(do nil)
                                 (range (- maxlen (count l))))))
                ls))))

(defn steralized [elem-info]
  "customized update for styles and callbacks"
  (walk/postwalk
           #(if (fn? %) "fn" %)
           (dissoc elem-info :style :on :children)))

(defn add-callbacks [el event-map]
  (doseq [[event-name f] event-map]
    (.addEventListener el event-name f)))

(defn rm-callbacks [el event-map]
  (doseq [[event-name f] event-map]
    (.removeEventListener el event-name f)))

(defn set-style [el styles]
  (doseq [[k v] styles]
    (aset (.-style el) k v)))

(defn mount [html-elem component]
  (loop [el html-elem] ; remove all children first
    (if (.hasChildNodes el)
      (do (.removeChild el (.-firstChild el))
          (recur el))))
  (let [node (component->VDomNode component nil)]
    (.appendChild
      html-elem
      @(:html-elem-atm node))
    node))
