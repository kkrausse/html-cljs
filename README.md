# html cljs

small library that has everything you need for making nice html.

demo timer component is available in demo.cljs

## Nouns

*Components* are functions

*Hooks* are higher-order functions that can you can attatch events to the dom
elements to

## Running / Publishing

run locally
  
    yarn shadow-cljs watch lib

publish locally

    lein install

publish to clojars

    lein deploy clojars
    # username is kkrausse and paste a clojars token for password

## TODO

- change hook and component to be records. That way it's tipesafe and using
  instance? you could fix the macro making it so that one could call hooks
  anywhere

- change the `(and (not= (hash old-component) (hash new-component))...` because it
  will not rerender if a totally different component has the same props, or
  will rerender completely if it's an inline component and props change even
  though they shouldn't, which will cause flicker
  this can be worked around by moving the inline component into it's own def though
