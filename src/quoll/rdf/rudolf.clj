(ns quoll.rdf.rudolf
  "RDF datatypes"
  {:author "Paula Gearon"}
  (:require [clojure.string :as s])
  (:import [java.io Writer]))

(defrecord IRI [iri prefix local]
  Object
  (toString [this]
    (if local
      (if prefix (str prefix \: local) (str \: local))
      (str \< iri \>))))

(defn iri
  ([i] (if (instance? IRI i) i (->IRI i nil nil)))
  ([i p l] (->IRI i p l)))

(def XSD-STRING-STR "http://www.w3.org/2001/XMLSchema#string")

(def XSD-STRING (->IRI XSD-STRING-STR "xsd" "string"))

(defn as-str
  [u]
  (if (instance? IRI u)
    (:iri u)
    (str u)))

(defrecord TypedLiteral [value datatype]
  Object
  (toString [this]
    (if (and datatype (not= (as-str datatype) XSD-STRING-STR))
      (str \" value "\"^^" datatype)
      (str \" value \"))))

(defrecord LangLiteral [value lang]
  Object
  (toString [this]
    (str \" value "\"@" lang)))

(defn lang-literal
  [value lang]
  (->LangLiteral value lang))

(defn typed-literal
  ([value] (->TypedLiteral value XSD-STRING))
  ([value datatype]
   (->TypedLiteral value datatype)))

(defrecord BlankNode [id]
  Object
  (toString [this]
    (str "_:" id)))

(let [counter (atom 0)]
  (def ^:private labelled-blank-node
    (memoize (fn [label] (->BlankNode (swap! counter inc)))))

  (defn blank-node
    ([] (->BlankNode (swap! counter inc)))
    ([label]
     (labelled-blank-node (if (s/starts-with? label "_:") (subs label 2) label)))))

(defn unsafe-blank-node
  [label]
  (->BlankNode (if (s/starts-with? label "_:") (subs label 2) label)))

(defmethod clojure.core/print-method quoll.rdf.rudolf.IRI [o, ^Writer w]
  (.write w (str o)))

(defmethod clojure.core/print-method quoll.rdf.rudolf.TypedLiteral [o, ^Writer w]
  (.write w (str o)))

(defmethod clojure.core/print-method quoll.rdf.rudolf.LangLiteral [o, ^Writer w]
  (.write w (str o)))

(defmethod clojure.core/print-method quoll.rdf.rudolf.BlankNode [o, ^Writer w]
  (.write w (str o)))
