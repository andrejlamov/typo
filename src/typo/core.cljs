(ns typo.core
  (:require [reagent.core :as r]
            [json-html.core :as h]
            [clojure.string :as string]))

(def ws (doto (js/WebSocket. "ws://localhost:3449/ws")
          (aset "onmessage" #(println (cljs.reader/read-string (aget % "data"))))
          (aset "onopen" #(js/console.log %))))

(defn send [d]
  (.send ws (pr-str d)))
(def state (r/atom {:expected-text "Hello world! How are you today?"
                    :idx 0
                    :actual-text ""}))

(defn typo-diff [actual-text expected-text idx]
  (let [pairs (map vector (concat actual-text (repeat nil)) expected-text)]
    (map-indexed (fn [char-idx [actual-char expected-char]]
                   {:expected expected-char
                    :actual actual-char
                    :space? (= expected-char " ")
                    :end? (= char-idx (count actual-text) idx)
                    :cursor? (= char-idx idx)
                    :correct? (= actual-char expected-char)})
                 pairs)))

(defn text[{:keys [idx actual-text expected-text]}]
    (->> (typo-diff actual-text expected-text idx)
         (map-indexed (fn [idx {:keys [space? cursor? actual correct? expected end?]}]
                        [:span.char {:key idx
                                     :style {:color (when cursor?
                                                      (cond end? "white"
                                                            correct? "green"
                                                            :else "red"))}
                                     :class [(when space? "space")
                                             (when cursor? "cursor")
                                             (when (and (not cursor?) actual)
                                               (if correct? "correct" "wrong"))]}
                         expected]))))

(defn input [st]
  [:textarea.input
   {:style {:opacity 0
            :padding 0
            :border 0
            :position "relative"}
    :on-key-down #(let [selectionStart (.. % -selectionStart)]
                    (swap! st assoc :idx selectionStart))
    :on-input #(let [v (.. % -target -value)]
                 (swap! st assoc :actual-text v))}])

(defn screen [st]
  (r/create-class
   {:component-will-update #(let [selectionStart (.. (r/dom-node %)
                                                     (querySelector ".input")
                                                     -selectionStart)]
                              (swap! st assoc :idx selectionStart))
    :component-did-mount #(let [input-elem (.. (r/dom-node %)
                                               (querySelector ".input"))
                                screen-elem (.. (r/dom-node %)
                                                (querySelector ".screen"))
                                height (.-offsetHeight screen-elem)
                                width (.-offsetWidth screen-elem)
                                input-style (.. input-elem -style)]
                            (set! (.-top input-style) (str (- height) "px"))
                            (set! (.-height input-style) (str height "px"))
                            (set! (.-width input-style) (str width "px")))
    :reagent-render (fn [] [:div
                            [:div.screen (text @st)]
                            [input st]])}))
(defn root []
  [:div [screen state]])

(defn render []
  (r/render [root]
            (js/document.getElementById "root")))

(render)
(defn on-js-reload []
  (render))

