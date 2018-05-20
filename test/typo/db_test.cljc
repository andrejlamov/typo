(ns typo.db-test
  (:require [datascript.core :as d]
            [typo.db :refer [schema]]
            [clojure.test :as t]))

(t/deftest scratch
  (let [conn (d/create-conn schema)]
    (d/transact! conn [{:component/name 'typo/main
                        :text/actual ""
                        :text/expected "hello world"}
                       {:component/name 'typo/second
                        :text/actual "2"}])(t/is (= {:db/id 1
                        :component/name 'typo/main
                        :text/actual ""
                        :text/expected "hello world"}
                       (d/pull (d/db conn)
                               '[*]
                               [:component/name 'typo/main])))

    (d/transact! conn [{:component/name 'typo/main
                        :text/actual "h"}
                       {:component/name 'typo/second
                        :text/actual "hi"}])

    (t/is (= {:text/actual "hi"}
             (d/pull (d/db conn)
                     '[:text/actual]
                     [:component/name 'typo/second])))

    (t/is (= {:text/actual "h"
              :text/expected "hello world"}
             (d/pull (d/db conn)
                     '[:text/actual :text/expected]
                     [:component/name 'typo/main])))))
