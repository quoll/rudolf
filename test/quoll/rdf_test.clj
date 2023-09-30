(ns quoll.rdf-test
  (:require [clojure.test :refer [testing is deftest]]
            [clojure.string :as s]
            [quoll.rdf :refer [iri as-str lang-literal typed-literal blank-node unsafe-blank-node]]))

(deftest test-iri
  (testing "do IRIs serialize as expected"
    (is (= "<http://test.com/>" (str (iri "http://test.com/"))))
    (is (= "tst:local" (str (iri "http://test.com/local" "tst" "local"))))))

(deftest test-typed-literals
  (testing "do TypedLiterals serialize as expected"
    (is (= "\"test\"" (str (typed-literal "test"))))
    (is (= "\"5\"^^xsd:double" (str (typed-literal "5" (iri "http://www.w3.org/2001/XMLSchema#double" "xsd" "double")))))
    (is (= "\"test\"^^<http://test.com/MyType>"
           (str (typed-literal "test" (iri "http://test.com/MyType")))))
    (is (= "\"another\\ntest\"" (str (typed-literal "another\ntest"))))))

(deftest test-lang-literals
  (testing "do LangLiterals serialize as expected"
    (is (= "\"test\"@en" (str (lang-literal "test" "en"))))
    (is (= "\"test\"@fr" (str (lang-literal "test" "fr"))))))

(deftest test-blank-nodes
  (testing "Are blank nodes built as expected"
    (let [b1 (blank-node)
          b2 (blank-node)
          b3 (blank-node "b1")
          b4 (blank-node "b1")
          b5 (blank-node "b5")]
     (is (not= b1 b2))
     (is (not= b1 b3))
     (is (= b3 b4))
     (is (not= b3 b5))
     (is (s/starts-with? (str b3) "_:b"))
     (is (s/starts-with? (str b4) "_:b"))
     (is (s/starts-with? (str b5) "_:b")))
    (is (= "_:b1" (str (unsafe-blank-node "b1"))))
    (is (= "_:b2" (str (unsafe-blank-node "_:b2"))))))
