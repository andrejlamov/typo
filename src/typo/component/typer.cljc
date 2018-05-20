(ns typo.component.typer
  (:require [datascript.core :as d]
            [clojure.string :as string]
            [typo.util :as u]
            [clojure.spec.alpha :as s]
            [clojure.core.match :as m]
            [clojure.core.async :as as :refer [put! <! sub pub chan go]]))

(def debounce 300)

(s/fdef delete-char
  :args (s/cat :text string?))
(defn delete-char [text]
  (->> text
       (drop-last 1)
       (string/join)))

(defn on-tick [name conn]
  (let [{:keys [cursor/visible?]}
        (d/pull (d/db conn)
                '[:cursor/visible?]
                [:component/name name])]
    (d/transact! conn [{:component/name name
                        :cursor/visible? (not visible?)}])))

(defn on-backspace [name conn]
  (let [{:keys [text/actual]}
        (d/pull (d/db conn)
                '[:text/actual]
                [:component/name name])]
    (d/transact! conn [{:component/name name
                        :text/actual (delete-char actual)
                        :cursor/visible? true}])))

(defn on-char [name conn c]
  (let [{:keys [text/actual]}
        (d/pull (d/db conn)
                '[:cursor/visible? :text/actual]
                [:component/name name])]
    (d/transact! conn [{:component/name name
                        :text/actual (str actual c)
                        :cursor/visible? true}])))

(defn io-consumer [name out-chan conn now-fn]
  (go (loop [future-blink (now-fn debounce)]
        (m/match (<! out-chan)
                 {:event "500ms-tick"} (if (< (now-fn) future-blink)
                                         (recur future-blink)
                                         (do (on-tick name conn)
                                             (recur (now-fn debounce)) ))
                 {:event "backspace"} (do (on-backspace name conn)
                                          (recur (now-fn debounce)))
                 {:event "char" :data c} (do (on-char name conn c)
                                             (recur (now-fn debounce)))))))

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

#?(:cljs
   (defn now
     ([addition] (+ addition (now)))
     ([] (.getTime (js/Date.)))))

#?(:cljs
   (defn spawn-io-consumer [name conn io-pub out-chan]
     (sub io-pub "io" out-chan)
     (io-consumer name out-chan conn now)))

(defn main [name conn]
   (let [{:keys [:text/actual
                :text/expected
                :cursor/visible?]}
        (d/pull (d/db conn) '[*] [:component/name name])]
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
