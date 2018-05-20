(ns typo.component-typer
  (:require [datascript.core :as d]
            [clojure.string :as string]
            [typo.util :as u]
            [typo.io :refer [io-pub]]
            [typo.db :refer [conn]]
            [clojure.spec.alpha :as s]
            [clojure.core.match :as m]
            [clojure.core.async :as as :refer [put! <! sub pub chan go]]))

(def debounce 300)

(defonce out-chan (chan))

(sub io-pub "io" out-chan)

;; init some data
(d/transact! conn [{:component/name 'typo/typer
                    :text/expected "hello world"}])

(s/fdef delete-char
  :args (s/cat :text string?))
(defn delete-char [text]
  (->> text
       (drop-last 1)
       (string/join)))

(defn tick [conn]
  (let [{:keys [cursor/visible?]}
        (d/pull (d/db conn)
                '[:cursor/visible?]
                [:component/name 'typo/typer])]
    (d/transact! conn [{:component/name 'typo/typer
                        :cursor/visible? (not visible?)}])))

(defn backspace [conn]
  (let [{:keys [text/actual]}
        (d/pull (d/db conn)
                '[:text/actual]
                [:component/name 'typo/typer])]
    (d/transact! conn [{:component/name 'typo/typer
                        :text/actual (delete-char actual)
                        :cursor/visible? true}])))

(defn on-char [conn c]
  (let [{:keys [text/actual]}
        (d/pull (d/db conn)
                '[:cursor/visible? :text/actual]
                [:component/name 'typo/typer])]
    (d/transact! conn [{:component/name 'typo/typer
                        :text/actual (str actual c)
                        :cursor/visible? true}])))

(defn io-consumer [now-fn conn]
  (go (loop [future-blink (now-fn debounce)]
        (m/match (<! out-chan)
                 {:event "500ms-tick"} (if (< (now-fn) future-blink)
                                         (recur future-blink)
                                         (do (tick conn)
                                             (recur (now-fn debounce)) ))
                 {:event "backspace"} (do (backspace conn)
                                          (recur (now-fn debounce)))
                 {:event "char" :data c} (do (on-char conn c)
                                             (recur (now-fn debounce)))))))

#?(:cljs
   (defn now
     ([addition] (+ addition (now)))
     ([] (.getTime (js/Date.)))))

#?(:cljs
   (defonce start (io-consumer now conn)))

(defn key-char [idx active-cursor?
                {:keys [key correct?]}]
  (let [fg-color (m/match [correct? active-cursor?]
                          [_ true] "white"
                          [true _] "green"
                          [false _] "red"
                          [_ _] "black")
        bg-color (cond
                   active-cursor? "black"
                   (and
                    (not (nil? correct?))
                    (= key " ")
                    (not correct?)) "red"
                   :else "white")]
    [:span {:key idx :style {:color fg-color :background bg-color}} key]))

(defn main [conn io-chan]
  (let [{:keys [:text/actual
                :text/expected
                :cursor/visible?]}
        (d/pull (d/db conn) '[*] [:component/name 'typo/typer])]
    [:div {:style
           {:position "relative"
            :margin "0 auto"
            :width "600px"
            :height "400px"}}
     (map-indexed (fn [idx d]
                    (key-char idx
                          (and visible?
                               (= idx (count actual)))
                          d))
                  (u/typo-diff expected actual))]))
