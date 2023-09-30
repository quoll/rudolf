(ns quoll.rdf
  "RDF datatypes"
  {:author "Paula Gearon"}
  (:require [clojure.string :as s])
  (:import [java.io Writer]
           [clojure.lang IPersistentCollection IPersistentMap IHashEq]
           [java.util Date]
           [java.net URL]))

(def common-prefixes
  "Common prefixes used in many datasets"
  {:rdf "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
   :rdfs "http://www.w3.org/2000/01/rdf-schema#"
   :xsd "http://www.w3.org/2001/XMLSchema#"
   :owl "http://www.w3.org/2002/07/owl#"
   :skos "http://www.w3.org/2004/02/skos/core#"
   :dcterms "http://purl.org/dc/terms/"})

(def ^:private magic 314159)

(deftype IRI [iri prefix local]
  Object
  (toString [this]
    (if local
      (if prefix (str prefix \: local) (str \: local))
      (str \< iri \>)))
  (hashCode [this] (+ magic (.hashCode iri)))
  (equals [this other] (.equiv this other))
  IPersistentCollection
  (equiv [this other]
    (and (= (class this) (class other))
         (= iri (.iri other))))
  IPersistentMap
  (valAt [this k] (.valAt this k nil))
  (valAt [this k not-found] (case k
                              :iri iri
                              :prefix prefix
                              :local local
                              not-found))
  IHashEq
  (hasheq [this] (+ magic (hash iri))))

(defn iri
  ([i] (if (instance? IRI i) i (->IRI i nil nil)))
  ([i p l] (->IRI i p l)))

(defn curie
  ([kw] (curie common-prefixes kw))
  ([context kw]
   (let [pref (keyword (namespace kw))
         prefix (or pref :default)
         nms (get context prefix)]
     (when-not nms
       (throw (ex-info (str "Namespace for " prefix " must be provided in the context")
                       {:prefix pref :context context})))
     (->IRI (str nms (name kw)) pref (name kw))))
  ([context prefix local]
   (let [pfx (or prefix :default)
         nms (get context pfx)]
     (when-not nms
       (throw (ex-info (str "Namespace for " pfx " must be provided in the context")
                       {:prefix prefix :context context})))
     (->IRI (str nms local) prefix local))))


(def XSD-STRING (curie common-prefixes :xsd/string))
(def XSD-INTEGER (curie common-prefixes :xsd/integer))
(def XSD-LONG (curie common-prefixes :xsd/long))
(def XSD-FLOAT (curie common-prefixes :xsd/float))
(def XSD-DATE (curie common-prefixes :xsd/date))
(def XSD-QNAME (curie common-prefixes :xsd/QName))
(def XSD-BOOLEAN (curie common-prefixes :xsd/boolean))
(def XSD-ANYURI (curie common-prefixes :xsd/anyURI))

(defn as-str
  [u]
  (if (instance? IRI u)
    (:iri u)
    (str u)))

(def echar-map {\newline "\\n"
                \return "\\r"
                \tab "\\t"
                \formfeed "\\f"
                \backspace "\\b"
                \" "\\\""
                \\ "\\\\"})

(defn print-escape
  "Escapes a string for printing"
  [s]
  (-> s
      (s/replace #"[\n\r\t\f\"\\]" #(echar-map (first %)))
      (s/replace "\b" "\\b")))

(defrecord TypedLiteral [value datatype]
  Object
  (toString [this]
    (if (and datatype (not= datatype XSD-STRING))
      (str \" (print-escape value) "\"^^" datatype)
      (str \" (print-escape value) \"))))

(defrecord LangLiteral [value lang]
  Object
  (toString [this]
    (str \" (print-escape value) "\"@" lang)))

(defn lang-literal
  [value lang]
  (->LangLiteral value lang))

(defn typed-literal
  ([value]
   (cond
     (string? value) (->TypedLiteral value XSD-STRING)
     (int? value) (->TypedLiteral (str value) XSD-INTEGER)
     (float? value) (->TypedLiteral (str value) XSD-FLOAT)
     (instance? Date value) (->TypedLiteral (str value) XSD-DATE)
     (keyword? value) (->TypedLiteral (str (namespace value) \: (name value)) XSD-QNAME)
     (boolean? value) (->TypedLiteral (str value) XSD-BOOLEAN)
     (uri? value) (->TypedLiteral (str value) XSD-ANYURI)
     (instance? URL value) (->TypedLiteral (str value) XSD-ANYURI)
     :default (throw (ex-info (str "Unknown datatype for typed literal: " (class value))
                              {:value value :type (class value)}))))
  ([value datatype]
   (let [dt (cond
              (instance? IRI datatype) datatype
              (string? datatype) (->IRI datatype)
              (or (instance? URL datatype) (uri? datatype)) (->IRI (str datatype)))]
   (->TypedLiteral value dt))))

(defrecord BlankNode [id]
  Object
  (toString [this]
    (str "_:" id)))

(let [counter (atom 0)]
  (def ^:private labelled-blank-node
    (memoize (fn [label] (->BlankNode (str \b (swap! counter inc))))))

  (defn blank-node
    ([] (->BlankNode (str \b (swap! counter inc))))
    ([label]
     (labelled-blank-node (if (s/starts-with? label "_:") (subs label 2) label)))))

(defn unsafe-blank-node
  [label]
  (->BlankNode (if (s/starts-with? label "_:") (subs label 2) label)))

(defmethod clojure.core/print-method quoll.rdf.IRI [o, ^Writer w]
  (.write w (str o)))

(defmethod clojure.core/print-method quoll.rdf.TypedLiteral [o, ^Writer w]
  (.write w (str o)))

(defmethod clojure.core/print-method quoll.rdf.LangLiteral [o, ^Writer w]
  (.write w (str o)))

(defmethod clojure.core/print-method quoll.rdf.BlankNode [o, ^Writer w]
  (.write w (str o)))

(def RDF-TYPE (curie common-prefixes :rdf/type))
(def RDF-FIRST (curie common-prefixes :rdf/first))
(def RDF-REST (curie common-prefixes :rdf/rest))
(def RDF-NIL (curie common-prefixes :rdf/nil))

