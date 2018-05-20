(ns typo.core
  (:require [rum.core :as rum]
            [typo.component-typer :as typer]
            [typo.db :refer [conn]]
            [datascript.core :as d]
            [typo.io :as io :refer [io-pub]]))

(rum/defc root []
  (typer/main conn io-pub))

(defn mount []
  (rum/mount (root) (. js/document getElementById "root")))

(d/listen! conn :mount #(mount))

(mount)

(defn on-js-reload []
  (println "state log" conn))
