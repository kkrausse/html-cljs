(ns html-cljs.hooks
  (:require [clojure.walk :as walk]))

(defmacro hook-meta
  "would use this for the below macro to find the hooks invoked in the body"
  [] `(symbol "html-cljs.html/hook"))

(defmacro with-hooks
  "attempt to have macro search for hook invocations in the body rather than
  have the hooks be explicitly passed. For now, a failed attempt since I am
  unable to resolve symbols at compile time and get the metadata on them."
  [funcname props & body]
  (println (hook-meta))
  (let [hooks (atom [])]
    (walk/prewalk
      (fn [form]
        (if (seq? form)
          (let [[head & tail] form
                thing (if (symbol? head)
                        (ns-resolve *ns* &env head)
                        head)]
            (println "checking" head )
            (println *ns*)
            (println "symbol? " (symbol? head))
            (if (symbol? head)
            (println "ns-resolved: " (ns-resolve *ns* &env head)))
            (println "meta thing" (meta thing))
            #_(if (symbol? head)
            (println "eval head" (eval head)))
            (if (and (some? head)
                     (contains? (meta thing) (hook-meta)))
              (swap! hooks #(conj % form)))))
        form)
      body)
    `(defn ~funcname ~props
       ~@body)))

; TODO: work with Hooks and Components as records with methods instead of just
; higher order functions

(defprotocol HookFunc
  (bind-lifecycle [_ cmp] "partially apply the hook. Returns an applied-hook"))

(defrecord Hook [f]
  HookFunc
  (bind-lifecycle [_ cmp] (f cmp)))

;(defprotocol BoundHook [f ])

;(def x (Hook. (fn [thing] (print "funy-" thing))))

;(instance? Hook (Hook. (fn [thing] (print "funy-" thing))))

#_(satisfies? HookFunc (Hook. (fn [thing] (print "funy-" thing))))

#_(defn hstuff [h]
  (bind-lifecycle h "thing hstuff"))


(defmacro mkhook [props hooks & body]
  (let [hooklhs (take-nth 2 hooks)
        hookrhs (take-nth 2 (rest hooks))
        hooked-syms (map (fn [hookname]
                           (gensym (str "hooked-" hookname)))
                         (map first hookrhs))
        clc-sym `clc#]
    `(fn [~clc-sym]
       (let [~@(mapcat vector
                       hooked-syms
                       (map (fn [hook] `(~hook ~clc-sym))
                            (map first hookrhs)))]
         (fn ~props
           (let [~@(mapcat vector
                        hooklhs
                        (map (fn [[hooked rhs]] `(~hooked ~@(rest rhs)))
                             (map vector hooked-syms hookrhs)))]
             ~@body))))))
