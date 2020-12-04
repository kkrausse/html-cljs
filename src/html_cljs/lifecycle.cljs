(ns html-cljs.lifecycle)

(defprotocol LifecycleHooks
  "passed to a component / hooks as the user interface to the library.
  The underlying dom element may be updated / deleted and recreated
  transparently without a full rerender."
  (rerender [this new-props]
           "call this to trigger refresh. used for hooks mainly when they
           want to re-trigger a render because of updating state. May
           delete and re-create the dom-element but will never delete the
           vdom node (nor can it if it wanted to since it doesn't have
           access to the original component function that made this node)")
  (getprops [this] "gets the props for refresh")
  (add-hook [this] "gets the context for a user hook. Each time the hook is called
                   for this component, this will return the same atom.")
  (on-mount [this f] "register callback on mounting")
  (on-destroy [this f] "calls f when the vdom element is destroyed"))

(defn refresh [clc]
  (rerender clc (getprops clc)))
