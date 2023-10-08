(ns quoll.rdf
  "RDF datatypes"
  {:author "Paula Gearon"}
  (:require [clojure.string :as s]
            #?(:clj [clojure.instant :as inst]
               :cljs [cljs.reader :as r]))
  #?(:clj (:import [java.io Writer]
                   [clojure.lang IPersistentCollection IPersistentMap IHashEq]
                   [java.util Date]
                   [java.net URL URI]
                   [java.text DateFormat])))

(def common-prefixes
  "Common prefixes used in many datasets"
  {:rdf "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
   :rdfs "http://www.w3.org/2000/01/rdf-schema#"
   :xsd "http://www.w3.org/2001/XMLSchema#"
   :owl "http://www.w3.org/2002/07/owl#"
   :skos "http://www.w3.org/2004/02/skos/core#"
   :dcterms "http://purl.org/dc/terms/"})

(def ^:private magic 314159)

#?(:clj
   (deftype IRI [iri prefix local]
     #?(:clj Object :cljs object)
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

   :cljs
   (deftype IRI [iri prefix local]
     Object
     (toString [this]
       (if local
         (if prefix (str prefix \: local) (str \: local))
         (str \< iri \>)))
     (equiv [this other] (-equiv this other))
     (get [this k not-found] (-lookup this k not-found))
     IHash
     (-hash [this] (+ magic (hash iri)))
     IEquiv
     (-equiv [this other]
       (and (= (type this) (type other))
            (= iri (.-iri other))))
     IAssociative
     (-assoc [this k v]
       (case k
         :iri (IRI. v prefix local)
         :prefix (IRI. iri v local)
         :local (IRI. iri prefix v)
         this))
     ILookup
     (-lookup [this k] (-lookup this k nil))
     (-lookup [this k not-found] (case k
                                   :iri iri
                                   :prefix prefix
                                   :local local
                                   not-found))
     IPrintWithWriter
     (-pr-writer [this writer opts]
       (print-map {:iri iri :prefix prefix :local local} pr-writer writer opts))))

(defn iri
  "Create an iri. If no prefix/namespace is available, then the prefix and local values may be nil.
  The full form of the IRI must always be available.
  (iri i): uses i as a full IRI string. Prefix and local are nil.
  (iri ctx p l): ctx is a map of prefixes as keywords to namespace strings.
                 p and l are the prefix and local values for the iri.
                 The keyword for p must appear as a key in the ctx map.
  (iri i p l): i is the full IRI string for the IRI, and p and l are the
               prefix:local pair for the CURIE form. l must match the tail of the IRI."
  ([i] (if (instance? IRI i) i (->IRI i nil nil)))
  ([i kw]
   (iri i (namespace kw) (name kw)))
  ([i p l]
   (if (map? i)
     (if-let [nms (if p
                    (get i (keyword p))
                    (or (get i "") (get i nil) (get i (keyword ""))))]
       (->IRI (str nms l) p l)
       (throw (ex-info (str "Prefix '" p "' not found in context") {:context i :prefix p :local l})))
     (let [i (str i)]
       (if (s/ends-with? i l)
         (->IRI i p l)
         (throw (ex-info "Local part of the IRI does not match the full IRI" {:iri i :local l})))))))

(defn curie
  "Create an IRI in Compact URI form, based on a keyword namespace/name.
  Since IRIs must always have the full form available then the IRI must appear in a common namespace,
  or else a context map containing the prefix must be provided."
  ([kw] (curie common-prefixes kw))
  ([context kw]
   (let [pref (namespace kw)
         prefix (if pref (keyword pref) :default)
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

(def xsd [:xsd/string :xsd/integer :xsd/long :xsd/float :xsd/date
          :xsd/QName :xsd/boolean :xsd/anyURI])

(def known-types (reduce #(assoc %1 %2 (curie common-prefixes %2)) {} xsd))

(def XSD-STRING (known-types :xsd/string))
(def XSD-INTEGER (known-types :xsd/integer))
(def XSD-LONG (known-types :xsd/long))
(def XSD-FLOAT (known-types :xsd/float))
(def XSD-DATE (known-types :xsd/date))
(def XSD-QNAME (known-types :xsd/QName))
(def XSD-BOOLEAN (known-types :xsd/boolean))
(def XSD-ANYURI (known-types :xsd/anyURI))

(defn as-str
  "Returns the string form of an IRI, not the serialization form.
  This means that the raw IRI is returned, and not an abbreviated form, nor is it wrapped in angle brackets."
  [u]
  (if (instance? IRI u)
    #?(:clj (:iri u) :cljs (.-iri u))
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
      (str \" (print-escape value) "\"^^" (if (keyword? datatype)
                                            (str (namespace datatype) \: (name datatype))
                                            datatype))
      (str \" (print-escape value) \"))))

(defrecord LangLiteral [value lang]
  Object
  (toString [this]
    (str \" (print-escape value) "\"@" lang)))

(defn lang-literal
  "Create a language coded literal. e.g. 'data'@en"
  [value lang]
  (->LangLiteral value lang))

#?(:clj
   (defn- inst-str
     [i]
     (let [^DateFormat fmt (.get (deref #'inst/thread-local-utc-date-format))]
       (.format fmt i)))
   :cljs
   (let [normalize (fn [n len]
                     (loop [ns (str n)]
                       (if (< (count ns) len)
                         (recur (str "0" ns))
                         ns)))]
     (defn- inst-str
       [i]
       (str
        (normalize (.getUTCFullYear i) 4)     "-"
        (normalize (inc (.getUTCMonth i)) 2)  "-"
        (normalize (.getUTCDate i) 2)         "T"
        (normalize (.getUTCHours i) 2)        ":"
        (normalize (.getUTCMinutes i) 2)      ":"
        (normalize (.getUTCSeconds i) 2)      "."
        (normalize (.getUTCMilliseconds i) 3) "-00:00"))))

(defn typed-literal
  "Converts a Clojure value into an RDF Literal. The datatype will be inferred if none is available."
  ([value]
   (cond
     (string? value) (->TypedLiteral value XSD-STRING)
     (int? value) (->TypedLiteral (str value) XSD-INTEGER)
     (float? value) (->TypedLiteral (str value) XSD-FLOAT)
     (instance? #?(:clj Date :cljs js/Date) value) (->TypedLiteral (inst-str value) XSD-DATE)
     (keyword? value) (->TypedLiteral (str (namespace value) \: (name value)) XSD-QNAME)
     (boolean? value) (->TypedLiteral (str value) XSD-BOOLEAN)
     (uri? value) (->TypedLiteral (str value) XSD-ANYURI)
     #?@(:clj [(instance? URL value) (->TypedLiteral (str value) XSD-ANYURI)])
     :default (throw (ex-info (str "Unknown datatype for typed literal: " (type value))
                              {:value value :type (type value)}))))
  ([value datatype]
   (let [dt (cond
              (instance? IRI datatype) datatype
              (string? datatype) (->IRI datatype nil nil)
              #?(:clj (or (instance? URL datatype) (uri? datatype))
                 :cljs (uri? datatype)) (->IRI (str datatype) nil nil)
              (keyword? datatype) (if (known-types datatype)
                                    datatype
                                    (throw (ex-info (str "Unknown datatype: " datatype)
                                                    {:datatype datatype :value value})))
              :default (throw (ex-info (str (type datatype) " cannot be converted to an IRI")
                                       {:value value :datatype datatype :type (type datatype)})))]
     (->TypedLiteral value dt))))

(defn to-clj
  "Converts an RDF Literal with a datatype into a native Clojure value"
  [{:keys [value datatype]:as literal}]
  (if-not datatype
    value
    (case #?(:clj (.iri datatype) :cljs (.-iri datatype))
      "http://www.w3.org/2001/XMLSchema#string" value
      "http://www.w3.org/2001/XMLSchema#integer" (parse-long value)
      "http://www.w3.org/2001/XMLSchema#float" (parse-double value)
      "http://www.w3.org/2001/XMLSchema#date" #?(:clj (inst/read-instant-timestamp value)
                                                 :cljs (r/parse-timestamp value))
      "http://www.w3.org/2001/XMLSchema#QName" (apply keyword (s/split value #":" 2))
      "http://www.w3.org/2001/XMLSchema#boolean" (parse-boolean value)
      "http://www.w3.org/2001/XMLSchema#anyURI" #?(:clj (URI. value)
                                                :cljs (goog.Uri. value))
      literal)))

(defrecord BlankNode [id]
  Object
  (toString [this]
    (str "_:" id)))

(let [counter (atom 0)]
  (def ^:private labelled-blank-node
    (memoize (fn [label] (->BlankNode (str \b (swap! counter inc))))))

  (defn blank-node
    "Create a blank node, using an optional label. The same label will always return the same blank node."
    ([] (->BlankNode (str \b (swap! counter inc))))
    ([label]
     (labelled-blank-node (if (s/starts-with? label "_:") (subs label 2) label)))))

(defn unsafe-blank-node
  "Return a new blank node object for a provided label. Reuse of a label *will* return a new blank node.
  This is for code that needs to manage its own blank node allocation."
  [label]
  (->BlankNode (if (s/starts-with? label "_:") (subs label 2) label)))

#?(:clj
   (defmethod clojure.core/print-method quoll.rdf.IRI [o, ^Writer w]
     (.write w (str o))))

#?(:clj
   (defmethod clojure.core/print-method quoll.rdf.TypedLiteral [o, ^Writer w]
     (.write w (str o))))

#?(:clj
   (defmethod clojure.core/print-method quoll.rdf.LangLiteral [o, ^Writer w]
     (.write w (str o))))

#?(:clj
   (defmethod clojure.core/print-method quoll.rdf.BlankNode [o, ^Writer w]
     (.write w (str o))))

(def RDF-TYPE (curie common-prefixes :rdf/type))
(def RDF-FIRST (curie common-prefixes :rdf/first))
(def RDF-REST (curie common-prefixes :rdf/rest))
(def RDF-NIL (curie common-prefixes :rdf/nil))

