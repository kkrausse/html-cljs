(ns html-cljs.html)

(defmacro component [hooks & body]
  (let [vdom-state-sym `vdom-state#
        instance-hooks `hook-instances#
        lhs (take-nth 2 hooks)
        rhs (take-nth 2 (rest hooks))]
   `(fn [~vdom-state-sym]
      (let [~instance-hooks [~@rhs]]
        (fn []
           ~(if (not-empty lhs)
             `(let [[~@lhs] (map #(% ~vdom-state-sym) ~instance-hooks)]
               ~@body)
             `(do ~@body)))))))

(defmacro cmp [hooks data & children]
  (let [vdom-state-sym `vdom-state#
        instance-hooks `hook-instances#
        lhs (take-nth 2 hooks)
        rhs (take-nth 2 (rest hooks))]
   `(fn [~vdom-state-sym]
      (let [~instance-hooks [~@rhs]]
        (fn []
           ~(if (not-empty lhs)
             `(let [[~@lhs] (map #(% ~vdom-state-sym) ~instance-hooks)]
               (assoc ~data :children (flatten [~@children])))
             `(assoc ~data :children (flatten [~@children]))))))))

