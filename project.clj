(defproject org.clojars.kkrausse/html-cljs "1.1.0"
  :description "small library that has everything you need for making nice html"
  :url "https://github.com/kkrausse/html-cljs"

  :dependencies
  ;; always use "provided" for Clojure(Script)
  [[org.clojure/clojurescript "1.10.520" :scope "provided"]]

  :deploy-repositories [["releases" :clojars]
                       ["snapshots" :clojars]]

  :repositories
  {"clojars" {:url "https://clojars.org/org.clojars.kkrausse/html-cljs"
              :sign-releases false}}

  :source-paths
  ["src"])
