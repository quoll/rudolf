(ns quoll.rdf-test
  (:require [clojure.test :refer [testing is deftest]]
            [clojure.string :as s]
            [quoll.rdf :as r :refer [iri as-str lang-literal typed-literal
                                     blank-node unsafe-blank-node to-clj]])
  #?(:clj (:import [java.net URI]
                   [clojure.lang ExceptionInfo])))

#?(:clj (defn uri [s] (URI. s))
   :cljs (defn uri [s] (goog.Uri. s)))

(deftest test-iri
  (testing "do IRIs serialize as expected"
    (is (= "<http://test.com/>" (str (iri "http://test.com/"))))
    (is (= "<http://test.com/>" (str (iri (uri "http://test.com/")))))
    (is (= "tst:local" (str (iri "http://test.com/local" "tst" "local"))))
    (is (thrown? ExceptionInfo (iri "http://test.com/locals" "tst" "local")))
    (is (= "tst:local" (str (iri "http://test.com/local" :tst/local))))
    (is (= "tst:local" (str (iri {:tst "http://test.com/"} :tst/local))))
    (is (= "http://test.com/local" (as-str (iri {:tst "http://test.com/"} :tst/local))))))

(deftest test-typed-literals
  (testing "do TypedLiterals serialize as expected"
    (is (= "\"test\"" (str (typed-literal "test"))))
    (is (= "\"5\"^^xsd:float" (str (typed-literal "5" (iri "http://www.w3.org/2001/XMLSchema#float" "xsd" "float")))))
    (is (= "\"5\"^^xsd:float" (str (typed-literal "5" :xsd/float))))
    (is (thrown? ExceptionInfo (typed-literal "5" 'int)))
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

(deftest test-autotyping
  (testing "Conversion of data into Literals"
    (is (= (typed-literal "x" r/XSD-STRING) (typed-literal "x")))
    (is (= (typed-literal "5" r/XSD-INTEGER) (typed-literal 5)))
    (is (= (typed-literal "2.5" r/XSD-FLOAT) (typed-literal 2.5)))
    (is (= (typed-literal "2023-10-01T21:54:06.349-00:00" r/XSD-DATE)
           (typed-literal #inst "2023-10-01T21:54:06.349-00:00")))
    (is (= (typed-literal "prefix:local" r/XSD-QNAME) (typed-literal :prefix/local)))
    (is (= (typed-literal "true" r/XSD-BOOLEAN) (typed-literal true)))
    (is (= (typed-literal "http://test.org/" r/XSD-ANYURI) (typed-literal (uri "http://test.org/"))))))

(deftest test-clojure-conversion
  (testing "Conversion of literals to Clojure data"
    (is (= "x" (to-clj (typed-literal "x" r/XSD-STRING))))
    (is (= 5 (to-clj (typed-literal "5" r/XSD-INTEGER))))
    (is (= 2.5 (to-clj (typed-literal "2.5" r/XSD-FLOAT))))
    (is (= #inst "2023-10-01T21:54:06.349-00:00"
           (to-clj (typed-literal "2023-10-01T21:54:06.349-00:00" r/XSD-DATE))))
    (is (= :prefix/local (to-clj (typed-literal "prefix:local" r/XSD-QNAME))))
    (is (= true (to-clj (typed-literal "true" r/XSD-BOOLEAN))))
    #?(:clj (is (= (uri "http://test.org/") (to-clj (typed-literal "http://test.org/" r/XSD-ANYURI))))
       :cljs (is (= (str (uri "http://test.org/")) (str (to-clj (typed-literal "http://test.org/" r/XSD-ANYURI))))))))

#?(:cljs (cljs.test/run-tests))
