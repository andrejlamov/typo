(ns typo.core
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.response :as response]
            [org.httpkit.server :refer [with-channel on-close on-receive send!] ]))

(defn ws-handler [request]
  (with-channel request channel
    (on-close channel (fn [status] (println "channel closed: " status)))
    (on-receive channel (fn [data] ;; echo it back
                          (println "channel received" data)
                          (send! channel data)))))

(defroutes app-routes
  (GET "/hello" [] "Hello compojure!")
  (GET "/ws" [] ws-handler)
  (route/resources "/" {:root "public"})
  (route/not-found "Not found"))

(def handler (wrap-reload (wrap-defaults #'app-routes site-defaults)))
