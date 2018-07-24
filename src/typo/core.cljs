(ns typo.core
  (:require [reagent.core :as r]
            [clojure.string :as string]))

(def state (r/atom {:expected-text "Hello world! How are you today?"
                    :actual-text ""}))

(defn typo-diff [actual-text expected-text]
  (let [pairs (map vector (concat actual-text (repeat nil)) expected-text)]
    (map-indexed (fn [idx [actual-char expected-char]]
                   {:expected expected-char
                    :actual actual-char
                    :space? (= expected-char " ")
                    :cursor? (= idx (count actual-text))
                    :correct? (= actual-char expected-char)})
                 pairs)))

(defn log [a]
  (println a)
  a)

(defn text[{:keys [actual-text expected-text]}]
    (->> (typo-diff actual-text expected-text)
         (map-indexed (fn [idx {:keys [space? cursor? actual correct? expected]}]
                        [:span.char {:key idx
                                     :class [(when space? "space")
                                             (when cursor? "cursor")
                                             (when actual (if correct? "correct" "wrong"))]}
                         expected]))))

(defn screen [st]
  (r/create-class
   {:component-did-mount #(let [input-elem (.. (r/dom-node %)
                                               (querySelector "input"))
                                screen-elem (.. (r/dom-node %)
                                                (querySelector "div"))
                                height (.-offsetHeight screen-elem)
                                width (.-offsetWidth screen-elem)
                                input-style (.. input-elem -style)]
                            (set! (.-height input-style) (str height "px"))
                            (set! (.-width input-style) (str width "px")))
    :reagent-render (fn [] [:div [:input {:type "text"
                                          :style {:opacity 0
                                                  :position "absolute"}
                                          :value (:actual-text @st)
                                          :on-change #(let [text (-> % .-target .-value)]
                                                        (swap! st assoc-in [:actual-text] text))}]
                            [:div.screen (text @st)]])}))



(defn typo [st]
  [:div [screen st]])

(defn root []
  [:div [typo state] [:p {}@state]])

(defn render []
  (r/render [root]
            (js/document.getElementById "root")))

(render)
(defn on-js-reload []
  (render))

