(ns typo.core
  (:require [rum.core :as rum]
            [typo.component.typer :as typer]
            [typo.db :refer [schema]]
            [datascript.core :as d]
            [clojure.core.async :as as :refer [put! <! sub pub chan go-loop]]
            [typo.io :as io]))


(defonce wire (do
                (def conn (d/create-conn schema))

                (def in-chan (chan))
                (io/init-producers in-chan)

                (def io-pub (pub in-chan :topic))

                (typer/spawn-io-consumer 'typo.component/typer1 conn io-pub (chan))

                (rum/defc root []
                  (typer/main 'typo.component/typer1 conn))

                (defn mount []
                  (rum/mount (root) (. js/document getElementById "root")))

                (d/listen! conn :mount #(mount))

                ;; init some data
                (d/transact! conn [{:component/name 'typo.component/typer1
                                    :text/expected "hello world"}])))
(mount)

(defn on-js-reload []
  (println "state log" conn))
