(ns build
  (:refer-clojure :exclude [test])
  (:require [clojure.tools.build.api :as b]
            [org.corfield.build :as bb]))

(def pom "build-rsc/pom.xml")
(def lib 'org.clojars.quoll/rudolf)
(def version "0.1.5")

;; clojure -T:build test
(defn test "Run the tests." [opts]
  (bb/run-tests opts))

;; clojure -T:build ci
(defn ci "Run the CI pipeline of tests (and build the JAR)." [opts]
  (-> opts
      (assoc :lib lib :version version :src-pom pom)
      (bb/run-tests)
      (bb/clean)
      (bb/jar)))

;; clojure -T:build install
(defn install "Install the JAR locally." [opts]
  (-> opts
      (assoc :lib lib :version version)
      (bb/install)))

;; clojure -T:build deploy
(defn deploy "Deploy the JAR to Clojars." [opts]
  (-> opts
      (assoc :lib lib :version version)
      (bb/deploy)))
