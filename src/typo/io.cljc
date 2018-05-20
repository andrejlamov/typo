(ns typo.io
  (:require
   [clojure.core.async :as as :refer [put! <! sub pub chan go-loop]]))

(def in-chan (chan))
(def out-chan (chan))
(def io-pub (pub in-chan :topic))

#?(:cljs
   (do
     (set! js/onkeydown
           (fn [e]
             (let [key (.-key e)]
               (cond
                 (= 1 (count key)) (put! in-chan {:topic "io" :event "char" :data key})
                 (= "Backspace" key) (put! in-chan {:topic "io" :event "backspace"})
                 :else nil))))

     (defn tick-fn []
       (put! in-chan {:topic "io" :event "500ms-tick"}))

     (defonce tick (.setInterval js/window (fn [] (#'tick-fn)) 500))))
