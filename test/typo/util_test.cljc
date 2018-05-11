(ns typo.util-test
  (:require
   [typo.util :as u]
   [clojure.string :as string]
   [clojure.data :as d]
   [clojure.test :as t]
   [clojure.spec.alpha :as s]
   [clojure.spec.test.alpha :as stest]
   [clojure.core.match :as m]))

(clojure.test/use-fixtures :once (fn [f] (stest/instrument) (f)))

(t/deftest str-to-coll-test
  (t/is (= [" " "1" " " "2"] (u/str-to-vec " 1 2")))
  (t/is (= [] (u/str-to-vec "")))
  (t/is (= ["h" "e" "y"] (u/str-to-vec "hey"))))

(t/deftest set-nil-tail-test
  (t/is (= ["y" "o" nil nil] (u/set-nil-tail ["y" "o"] 4)))
  (t/is (= ["y"] (u/set-nil-tail ["y"] 0))))

(t/deftest zip-fill-nil-test
  (let [[a b c] (d/diff (u/str-to-vec "hello") (u/str-to-vec "yel"))
        [a' b' c' _ _] (u/zip-fill-nil a b c)]
    (t/is (= [["h" "y" nil] [nil nil "e"] [nil nil "l"]] [a' b' c']))))

(t/deftest typo-diff-test
  (t/is (= [{:key "h" :correct? true}
            {:key "e" :correct? false}
            {:key "l" :correct? true}
            {:key "l" }
            {:key "o" }]
           (u/typo-diff "hello" "hwl")))
  (t/is (= [{:key "h" :correct? true}
            {:key "e" :correct? true}
            {:key "l" :correct? true}
            {:key "l" :correct? true}
            {:key "o" :correct? true}]
           (u/typo-diff "hello" "hello")))
  (t/is (= [{:key "h" :correct? false}
            {:key "e" :correct? false}
            {:key "l" }
            {:key "l" }
            {:key "o" }]
           (u/typo-diff "hello" " 1")))
  (t/is (= [
            {:key "h" :correct? true}
            {:key "e" :correct? true}
            {:key "l" :correct? true}
            {:key "l" }
            {:key "o" }]
           (u/typo-diff "hello" "hel"))))
