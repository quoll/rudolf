(ns quoll.rdf-test
  (:require [clojure.test :refer [testing is deftest]]
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
           (str (typed-literal "test" (iri "http://test.com/MyType")))))))

(deftest test-lang-literals
  (testing "do LangLiterals serialize as expected"
    (is (= "\"test\"@en" (str (lang-literal "test" "en"))))
    (is (= "\"test\"@fr" (str (lang-literal "test" "fr"))))))
