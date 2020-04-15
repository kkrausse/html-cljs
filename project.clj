(defproject org.clojars.kkrausse/html-cljs "1.0.4-SNAPSHOT"
  :description "small library that has everything you need for making nice html"
  :url "https://maven.pkg.github.com/kkrausse"

  :dependencies
  ;; always use "provided" for Clojure(Script)
  [[org.clojure/clojurescript "1.10.520" :scope "provided"]]

  :repositories
  {"clojars" {:url "https://maven.pkg.github.com/kkrausse"
              :sign-releases false}}
  :source-paths
  ["src"])
