(ns typo.core
  (:require [reagent.core :as r]
            [json-html.core :as h]
            [clojure.string :as string]))

'(def ws (doto (js/WebSocket. "ws://localhost:3449/ws")
          (aset "onmessage" '#(println (cljs.reader/read-string (aget % "data"))))
          (aset "onopen" '#(js/console.log %))))

'(defn send [d]
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

(defn text [{:keys [idx actual-text expected-text]}]
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

(defn handle-text-input [current-text prev-text current-idx prev-idx]
  (cond
    ;; one char input
    (= 1 (- (count current-text) (count prev-text)))
    (let [adjusted-prev-idx (if (= 1 (- prev-idx current-idx) )
                              prev-idx
                              (dec current-idx))]
      {:idx-prev adjusted-prev-idx
       :actual-text (string/join
                     (assoc (vec current-text)
                            (inc adjusted-prev-idx) ""))})
    ;; keyboard autocomplete/paste
    (and (> (count current-text) (count prev-text))
         (not= current-idx (count current-text)))
    (let [delta-count (- (count current-text) (count prev-text))
          current-vec (vec current-text)
          a (subvec current-vec 0 current-idx)
          b (subvec current-vec (+ current-idx delta-count))
          c (concat a b)]
      {:actual-text (string/join c)})

    :else
    {:actual-text current-text}))

(defn input [st]
  (let [diff-view (:diff-view @st)]
    [:textarea.input
     {:style
      {:opacity 0
       :padding 0
       :border 0
       :top (str (- (:height diff-view)) "px")
       :height (str (:height diff-view) "px")
       :width (str (:width diff-view) "px")
       :position "relative"}
      :type "text"
      :on-key-down #(let []
                      (swap! st merge {:_force-rerender (js/Date.now)}))
      :on-input #(let [target (.. % -target)
                       current-text (.. target -value)
                       prev-idx (:idx @st)
                       prev-text (:actual-text @st)
                       current-idx (aget target "selectionStart")
                       sub-state (handle-text-input current-text prev-text current-idx prev-idx)]
                   (swap! st merge sub-state)
                   (aset target "value" (:actual-text @st))
                   (aset target "selectionStart" current-idx)
                   (aset target "selectionEnd" current-idx))}]))

(defn get-diff-view-size [o]
  (let [screen-elem (.. (r/dom-node o)
                        (querySelector ".diff"))]
    {:height (.-offsetHeight screen-elem)
     :width (.-offsetWidth screen-elem)}))

(defn get-input-caret-idx[o]
  (.. (r/dom-node o)
      (querySelector ".input")
      -selectionStart))

(defn screen [st]
  (r/create-class
   {:component-will-update #(do (swap! st merge {:diff-view (get-diff-view-size %)})
                                (swap! st assoc :idx (get-input-caret-idx %)))
    :component-did-mount #(swap! st merge {:diff-view (get-diff-view-size %)})
    :reagent-render (fn [] [:div.screen
                            [:div.diff (text @st)]
                            [input st]])}))
(defn root []
  [:div [screen state] (h/edn->hiccup @state)])

(defn render []
  (r/render [root]
            (js/document.getElementById "root")))

(render)
(defn on-js-reload []
  (render))

