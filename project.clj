(defproject org.clojars.freeagent/clj-amazon "0.2.2-SNAPSHOT"
  :description "Clojure bindings for the Amazon Product Advertising API."
  :url "http://github.com/FreeAgent/clj-amazon/"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  :plugins [[lein-autodoc "0.9.0"]]
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [clj-http "0.5.8"]]
  :autodoc {:name "clj-amazon"
            :description "Clojure bindings for the Amazon Product Advertising API."
            :copyright "Copyright 2011~2012 Eduardo Julian"
            :web-src-dir "http://github.com/FreeAgent/clj-amazon/src/"
            :web-home "http://github.com/FreeAgent/clj-amazon/"
            :output-path "autodoc"}
)
