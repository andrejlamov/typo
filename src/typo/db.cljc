(ns typo.db
  (:require [datascript.core :as d]))

(def schema {:component/name {:db/unique :db.unique/identity}
             :text/actual nil
             :text/expected nil
             :cursor/visible? nil})


