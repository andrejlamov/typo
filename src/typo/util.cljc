(ns typo.util 
  (:require
   [clojure.string :as string]
   [clojure.data :as d]
   [clojure.spec.alpha :as s]
   [clojure.spec.test.alpha :as stest]
   [clojure.core.match :as m]))

(s/fdef set-nil-tail
        :args (s/cat :coll coll? :total-len int?))
(defn set-nil-tail [coll total-len]
  (let [rest (-> total-len
                 (- (count coll))
                 (repeat nil)
                 (vec))]
    (concat coll rest)))

(s/fdef str-to-vec
        :args (s/cat :text string?))
(defn str-to-vec [text]
  (if (= "" text)
    []
    (let [coll (string/split text #"")
          [fst & rest] coll]
      (if (= "" fst) rest coll))))

(s/fdef zip-fill-nil
        :args (s/cat :a coll? :b coll? :c coll?))
(defn zip-fill-nil [a b c]
  (let [max-count (max (count a) (count b) (count c))
        a' (set-nil-tail a max-count)
        b' (set-nil-tail b max-count)
        c' (set-nil-tail c max-count)
        ]
    (map vector a' b' c')))

(s/fdef typo-diff
        :args (s/cat :expected string? :actual string?))
(defn typo-diff [expected actual]
  (let [diff (d/diff (str-to-vec expected) (str-to-vec actual))
        [only-a only-b both] (map #(if (nil? %) [] %) diff)
        zipped (zip-fill-nil only-a only-b both)]
    (map
     #(m/match %
              [nil nil a] {:key a :correct? true}
              [a   nil _] {:key a}
              [a   _ nil] {:key a :correct? false}
              )
     zipped)))
