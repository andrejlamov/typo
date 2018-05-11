(defproject typo "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"

  :min-lein-version "2.7.1"

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.238"]
                 [org.clojure/core.async "0.4.474"]
                 [org.clojure/core.match "0.3.0-alpha5"]
                 [org.clojure/test.check "0.9.0"]
                 [rum "0.11.2"]]

  :plugins [[lein-figwheel "0.5.16"]
            [lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]]

  :source-paths ["src"]

  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src"]

                :figwheel {:on-jsload "typo.core/on-js-reload"
                           :open-urls ["http://localhost:3449/index.html"]
                                        ;:load-warninged-code true
                           }

                :compiler {:main typo.core
                           :asset-path "js/compiled/out"
                           :output-to "resources/public/js/compiled/typo.js"
                           :output-dir "resources/public/js/compiled/out"
                           :source-map-timestamp true
                           :preloads [devtools.preload]}}
               ;; You can build this with:
               ;; lein cljsbuild once min
               {:id "min"
                :source-paths ["src"]
                :compiler {:output-to "resources/public/js/compiled/typo.js"
                           :main typo.core
                           :optimizations :advanced
                           :pretty-print false}}]}

  :figwheel {
             :css-dirs ["resources/public/css"] ;; watch and update CSS
             }


  :profiles {:dev {:dependencies [[binaryage/devtools "0.9.9"]
                                  [figwheel-sidecar "0.5.15"]
                                  [com.cemerick/piggieback "0.2.2"]]
                   :source-paths ["src"]
                   :plugins [[cider/cider-nrepl "0.17.0"]]
                   :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                   :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                                     :target-path]}})
