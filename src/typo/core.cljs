(ns typo.core
  (:require [rum.core :as rum :refer [react] :rename {react r}]
            [clojure.spec.alpha :as s]
            [clojure.core.match :as m]
            [clojure.string :as string]
            [clojure.spec.test.alpha :as stest]
            [clojure.core.async :as as :refer [<! put! chan go-loop]]
            [typo.util :as u]))

(stest/instrument)

(def state (atom {:active-cursor? true 
                  :actual ""
                  :expected "hello world hello world hello world hello world"}))


(def io-chan (chan))

(def debounce 300)
(def blink-timeout 550)

(s/fdef delete-char 
  :args (s/cat :text string?))
(defn delete-char [text]
  (->> text
       (drop-last 1)
       (string/join)))

(defn now
  ([addition] (+ addition (now)))
  ([] (.getTime (js/Date.))))

(def io-consumer (go-loop [future-blink (now)]
                   (m/match (<! io-chan)
                            ["tick"] (if (< (now) future-blink)
                                       (recur future-blink)
                                       (swap! state update-in [:active-cursor?] not))
                            ["backspace"] (do (swap! state assoc :active-cursor? true)
                                              (swap! state update-in [:actual] delete-char)) 
                            ["char" c] (do (swap! state assoc :active-cursor? true)
                                           (when (< (->> @state :actual count)
                                                    (->> @state :expected count))
                                             (swap! state update-in [:actual] str c))))
                   (recur (now debounce))))

(defn char-key [idx active-cursor?
                {:keys [key correct?] :as letter}]
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
                   true "white")]
    [:span {:key idx :style {:color fg-color :background bg-color}} key]))

(rum/defc root < rum/reactive
  ([] (root state))
  ([state]
   (let [{:keys [expected actual active-cursor?]} (r state)]
     [:div {:style
            {:position "relative"
             :margin "0 auto"
             :width "600px"
             :height "400px"}}
      (map-indexed (fn [idx d]
                     (char-key idx (and active-cursor?
                                        (= idx (count actual))) d))
                   (u/typo-diff expected actual))])))

(rum/mount (root) (. js/document getElementById "root"))

(set! js/onkeydown
      (fn [e]
        (let [key (.-key e)]
          (cond
            (= 1 (count key)) (put! io-chan ["char" key])
            (= "Backspace" key) (put! io-chan ["backspace"])
            :else nil)))) 

(defn tick-fn []
  (put! io-chan ["tick"]))

(defonce tick (.setInterval js/window (fn [] (#'tick-fn)) blink-timeout))



(defn on-js-reload []
  (println "state log" @state))
