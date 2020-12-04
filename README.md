# html cljs

small library for making nice html. Inspired by React js.

demo timer component is available in demo.cljs

## Nouns

*Hook* a function that takes in props and uses the dynamically bound `*clc*`
  variable to hook into the components lifecycle. the ComponentLifecycle has
  add-hook method defined on it that gives the hook access to data.
  The way this works requires (like React js) hooks to be called unconditionally
  each time the component is called.

*Components* Regular functions that return an ElementInfo record

*ElementInfo* is a record that has all the information needed to create a dom element.
Children is a sequence of Components.

## Running / Publishing

run locally
  
    yarn shadow-cljs watch lib

publish locally

    lein install

publish to clojars

    lein deploy clojars
    # username is kkrausse and paste a clojars token for password

## TODO

- change the `(and (not= (hash old-component) (hash new-component))...` because it
  will not rerender if a totally different component has the same props, or
  will rerender completely if it's an inline component and props change even
  though they shouldn't, which will cause flicker
  this can be worked around by moving the inline component into it's own def though
