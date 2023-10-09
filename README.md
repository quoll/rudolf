# RuDolF
RDF datatypes.

## Usage
### Leiningen/Boot
```clojure
[org.clojars.quoll/rudolf "0.1.8"]
```
### Clojure CLI/deps.edn
```clojure
org.clojars.quoll/rudolf {:mvn/version "0.1.8"}
```

## Rationale
RDF is about data portability, meaning that lots of applications need to represent the same data objects,
whether they are parsing, writing, storing, or querying data. While there are some common ways of
representing this data, each different program often ends up using its own internal objects, records,
interfaces, protocols, modules, etc.
<img src="https://github.com/quoll/rudolf/assets/358875/348bdfaf-a82c-45c4-b2dc-82a153a8b162" alt="RuDolF" align="right"/>

A common form used in the Java landscape is Jena's
[RDFDatatypes](https://github.com/apache/jena/blob/main/jena-core/src/main/java/org/apache/jena/datatypes/RDFDatatype.java).
While relatively general purpose, these are focused on Jena's requirements, which are not always applicable
to other applications. They are also part of Java, making them less useful when writing code that is
cross-compatible for ClojureScript applications.

This package is simply a standard set of records that can be used across Clojure and ClojureScript applications.

There is a single protocol: `Serializable`, with the function `serialize`. This function converts the object
into a string form that is suitable for [Turtle](https://www.w3.org/TR/turtle/),
[NTriples](https://www.w3.org/TR/n-triples/) or [SPARQL](https://www.w3.org/TR/sparql11-overview/).

### Types
RDF describes a [graph](https://en.wikipedia.org/wiki/Graph_(discrete_mathematics)) that uses nodes of 3 types:
- [IRI](#iri)
- [Literal](#literal)
- [Blank Node](#blank-nodes)

These types are all described below, along with the semantics of using them.

## Contents
- [IRIs](#iri)
  * [IRI Context](#iri-context)
  * [IRI Representation](#iri-representation)
  * [IRI Construction](#iri-construction)
  * [Working with IRIs](#working-with-iris)
- [Literals](#literals)
  * [Literal Representation](#literal-representation)
    - [Language-Tagged Literal Representation](#language-tagged-literal-representation)
    - [Typed Literal Representation](#typed-literal-representation)
  * [Literal Construction](#literal-construction)
  * [Working with Literals](#working-with-literals)
    - [Language-Tagged Literals](#language-tagged-literals)
    - [Typed Literals](#typed-literals)
- [Blank Nodes](#blank-nodes)
  * [Blank Node Representation](#blank-node-representation)
  * [Blank Node Construction](#blank-node-construction)
  * [Working with Blank Nodes](#working-with-blank-nodes)
- [Examples](#examples)
- [TO DO](#todo)

## IRI
IRIs are used as labeled nodes in an RDF graph.

IRIs are a supertype of [URI](https://en.wikipedia.org/wiki/Uniform_Resource_Identifier), which allows for
internationalized characters in the domain. Most programming languages support a native URI type, with no
constraints on the characters permitted, making them _de facto_ IRI implementations. Many people
(especially in the English-speaking world) will use the term URI rather than IRI.

While IRIs usually describe a location online, this is actually the subtype of identifier called the
[URL](https://en.wikipedia.org/wiki/URL). The IRI, or URI, is a generalized identifier, and need not
necessarily describe a location on the internet. This applies, even if it appears as a valid URL, though
[it is considered a Best Practice](https://www.w3.org/TR/cooluris/) to have URIs that refer to locations
where information about a resource can be retrieved.

### IRI Context
Other valid URI forms also exist, including the [URN](https://en.wikipedia.org/wiki/Uniform_Resource_Name),
which contains a numerical identifier, and the _URI Reference_, which may appear without a scheme or host.
In fact a URI Reference allows for almost any string to be a valid URI, provided it does not contain illegal
characters. This makes URIs more flexible than most applications expect, and makes validation in the general
case impossible. This means that the best representation of a URI is typically a standard string. URI
references are _relative_ and not _absolute_, and only make sense when there is some context that can allow
them to be resolved to an absolute URI. This context will be provided by a _base_. For instance, `/path`
is a relative URI reference, and if it were in a context where the base is set to `http://localhost`, then
the relative URI can be resolved to the absolute URI of `http://localhost/path`.

IRIs can also be represented in many syntaxes as a [Qualified Name (QName)](https://en.wikipedia.org/wiki/QName)
or [CURIE (Compact URI)](https://en.wikipedia.org/wiki/CURIE) (these are similar, though QNames are a subtype
of CURIE, and are constrained to characters that can represent [XML](https://en.wikipedia.org/wiki/XML) names).
In this form, they appear as a _prefix_ and _local name_. Similarly to relative URIs, this form is only
valid with some context that allows the complete, absolute URI to be determined. In this case, the context
must contain a mapping of a _prefix_ to a _namespace_, which is an absolute URI. If the _prefix_ on
a CURIE is not in the current context, then the CURIE is not valid.

As an example, the CURIE `rdf:type` is only valid when the context maps the prefix `rdf` to a namespace.
This is almost always going to be the standard RDF namespace of: `http://www.w3.org/1999/02/22-rdf-syntax-ns#`.
The absolute URI is this namespace followed by the local name, which in this case is:
`http://www.w3.org/1999/02/22-rdf-syntax-ns#type`.

Clojure's keywords also have a form of `:namespace/local` which is very similar to a CURIE representation of
and IRI. Like many Clojure implementations, Rudolf takes advantage of this, and will often represent a CURIE
using a keyword, such as `:rdf/type`. Note that keywords do not have any context, and they should only be
used when the context is clear.

Rudolf contains a set of common prefixes, as a map from the keyword of the common prefix to a string. This is
stored as `quoll.rdf/common-prefixes` and has the keys:
- `:rdf`
- `:rdfs`
- `:xsd`
- `:owl`
- `:skos`
- `:dcterms`

Context maps will always have this form of _keyword_→_iri-string_. RDF documents may sometimes refer to a
_default_ namespace with an empty prefix. This can be provided via a key that is one of the following:
- `nil`
- `""` _(the empty string)_
- `(keyword "")` _(an empty keyword)_
- `:_default`

_Choose whichever works best for your application._
### IRI Representation
IRIs must always contain their complete, absolute form as a string. They do not contain a reference to
any local context, but a context can be used when constructing one, and they may contain their `prefix`/`local`
abbreviation.

When an IRI can be represented as a CURIE (via _prefix/local_), then it will be serialized this way.
For instance: `rdf:type`

When a prefix/local is not available, then it will be serialized in the absolute form, which is enclosed
with "angle brackets. For instance: `<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>`

Internally, an IRI contains:
- The absolute form of the IRI as a string
- An optional prefix for CURIE form of the IRI, as a string.
- An optional local name for the CURIE for of the IRI, as a string. This does not require a prefix (in which case the CURIE uses the default prefix), but it must be present if the prefix is present.

### IRI Construction
Creating an IRI is done via the `iri` or `curie` functions. The absolute IRI must be provided in each case:
- `quoll.rdf/iri`: There are 3 forms of this
  - `(iri string)`: This takes the string form of the absolute IRI and returns a simple object with no prefix or local parts.
  - `(iri string keyword)`: This takes the string form of the absolute IRI, and a keyword. The keyword must match the IRI, meaning that the local name must match the end of IRI string.
  - `(iri string string string)`: This takes the string form of the absolute IRI, the string form of the prefix, and the string form of the local name. The local name must match the end of the IRI string.
- `quoll.rdf/curie`: This creates an IRI from a _prefix/name_ in a context. The context is always expected to be a map of keywords to IRI strings.
  - `(curie keyword)`: This uses a keyword as an ersatz CURIE, using the `common-prefixes` to resolve the namespace.
  - `(curie context keyword)`: This uses a keyword as an ersatz CURIE, using the provided context to resolve the namespace.
  - `(curie context prefix local)`: This creates an IRI using a CURIE prefix and local name, using the provided context to resolve the namespace.

### Working with IRIs
Using `serialize` to write an IRI in its string form will a CURIE or absolute representation, depending on its elements.
To get the raw IRI form, use the `as-str` function.

IRIs with the same absolute form will always compare as equal, even if the prefix/local values differ.

## Literals
Literals are of 2 types: Language-tagged Literals, and Typed Literals. In both cases, they hold a pair of values.

A Language-tagged Literal is a string, with a [language tag](https://www.rfc-editor.org/rfc/rfc5646)
(represented by a string). These have an implicit datatype of `rdf:langString`, and the language tag _**must**_
be present, and must conform to the specification in [section 2.2.9 of RFC 5646](https://www.rfc-editor.org/rfc/rfc5646#section-2.2.9).

A Typed Literal has a string representation of a value, along with the URI for the datatype of the value.
If no datatype is provided with Literal, then the assumption is that the literal a string, with the
datatype `xsd:string`. RDF systems are expected to support datatypes from [XML Schema](https://www.w3.org/TR/xmlschema-2/),
but other datatypes are allowed.

### Literal Representation
While some systems have a single type of Literal form (where the language tag is empty unless the datatype is `rdf:langString`)
Rudolf separates them. This is because they behave quite differently to each other, both in serialization and
in their requirements.

#### Language-Tagged Literal Representation
Language-tagged literals hold a string value, and a language-tag as a string. These are serialized
as the string between quotes, followed by an `@` symbol, and the language tag. The language tag may
only contain letters, numbers, and dashes. For instance, here are 3 separate language-tagged literals:
```
"The color is red"@en
"The colour is red"@en-uk
"La couleur est rouge"@fr
```
#### Typed Literal Representation
Typed literals have a string representing the lexical form of the literal, and a URI for the datatype.
Many datatypes have various lexical representations of the same value, and the lexical form need
not be canonicalized.

Typed literals are serialized as the lexical form in quotes, followed by the characters `^^`, and then
the IRI of the datatype. Note that the datatype IRI is serialized according to standard IRI serialization,
so it may be absolute or a CURIE. If the datatype is `xsd:string` then the type IRI may be omitted.

For instance, assuming the prefix `xsd` is defined appropriately in the context, the following
3 IRIs are equivalent:
```
"Hello world"
"Hello world"^^xsd:string
"Hello world"^^<http://www.w3.org/2001/XMLSchema#string>
```

Some datatypes allow various lexical forms for a single value. These should be considered identical
literals, even when the lexical form differs. This can be tedious to implement, even if staying
with the more common XSD types. To avoid this work, several of the known types are kept in their
canonical form (`xsd:integer`, `xsd:float`, `xsd:dateTime`, `xsd:boolean`).

The numerical types and boolean values have a special form of serialization, in that they
are serialized in their standard lexical form without any syntactic markers. Consequently,
The following pairs are both valid serializations of each other:

| Typed Literal | Standard |
|---------------|----------|
|"5"^^xsd:integer | 5 |
|"5.1"^^xsd:float | 5.1 |
|"false"^^xsd:boolean | false |

### Literal Construction
Language-tagged Literals are constructed using `quoll.rdf/lang-literal`:
- `(lang-literal string string)`: This contains the lexical string of the literal, and the string with the language code.

Typed Literals are constructed using `quoll.rdf/typed-literal`:
- `(typed-literal value)`: This takes a Clojure value, and will convert it into a literal with the appropriate datatype. If the datatype is not recognized, then an exception will be thrown.
- `(typed-literal value iri)`: This takes a value or its lexical form as a string, and an IRI representing the datatype. The IRI must be a string, a URI object, a Rudolf IRI, or a keyword. If a keyword is used, then it must be one of the known XSD datatypes.

The supported Clojure types and their known XSD datatypes are:

|Clojure Type|XSD Type|ClojureScript|
|------------|--------|-------------|
|`String`|`xsd:string`| ✅ |
|`long`|`xsd:integer`| ✅ |
|`double`|`xsd:float`| ✅ |
|`Date`|`xsd:dateTime`| ✅ |
|`Instant`|`xsd:dateTime`|  |
|`Keyword`|`xsd:QName`| ✅ |
|`Boolean`|`xsd:boolean`| ✅ |
|`URI`|`xsd:anyURI`| ✅ |
|`URL`|`xsd:anyURI`|  |

Note that ClojureScript does not have all of the types that Clojure supports, and all ClojureScript numbers are doubles.
However, any numerical value will be recognized as an integer if the function `clojure.core/int?` returns true.

Despite not being part of the general Clojure ecosystem, Rudolf also recognizes the Java types of
`int`, `short`, and `byte` values as `xsd:integer`, as well as their boxed equivalents. Similarly,
`float` is recognized as `xsd:float`.

### Working with Literals
For simple data types (strings and numbers), wrapping values in a call to `typed-literal` will be all that's needed.
If a different datatype is ever needed, pass it in explicitly.

#### Language-tagged Literals
Language-tagged literals will always be written in `"lexical"@lang` form.

Language-tagged literals are a record containing 2 fields:
- `:value` always a string
- `:lang` always a string

#### Typed Literals
Typed literals will always write themselves in the `"lexical"^^type` form, unless the type is `xsd:string`, in
which case the literal is written in `"lexical"` form. Systems that want to write these literals differently
should choose their own mechanism.

Typed literals are a record containing 2 fields:
- `:value` always a string
- `:datatype` always an IRI

## Blank Nodes
Blank Nodes are graph elements with a label and have no inherent semantic meaning. They typically appear
in a document using a label, but this label is only used to identify when the same node needs to be
used again in the document. A blank node's label has no meaning outside of a document. This is important
to consider, since many documents may use the same labels for their blank nodes, but if those
documents are imported to the same place, the nodes will be distinct from each other.

Because of this, the user may want to manage the internal identifiers for blank nodes themselves.

### Blank Node Representation
Blank nodes often appear with a label that looks like a CURIE with an underscore as a namespace,
e.g. `_:b0`. In some contexts, they may appear as a structure without any such label, either
explicitly, as in the `[]` syntax of Turtle, or implicitly, as when they are created in an `rdf:List`.

When blank nodes are parsed, they may come with a label. The object created to represent that
blank node should be recorded so that future reference to the same label in that context
will refer to the same node. Note that parsing 2 different documents will mean 2 separate contexts.

### Blank Node Construction
Blank nodes can be constructed using `quoll.rdf/blank-node`.
- `(blank-node)`: Creates a new unique blank node.
- `(blank-node string)`: Creates a blank node using the provided label. If the label has been seen before, then the original blank node that was created for that label is returned.

Many systems need to manage their own blank nodes, particularly if there are numerous contexts.
`quoll.rdf/unsafe-blank-node` was created for this purpose:
- `(unsafe-blank-node label)`: Creates a new blank node, storing the label internally.

### Working with Blank Nodes
Blank nodes use an internal counter to generate labels of the form: `_:b0`, `_:b1`, `_:b2`...

Provided labels are not used in the blank node record, but are instead used to lookup the cache for them.
Using `unsafe-blank-node` avoids the cache, and instead saves the label as the blank node's ID.
If an underscore/colon prefix is provided as the label, then this will be removed internally.

Blank nodes are a record with a single field:
- `:id`

When serializing, blank nodes are always represented as `_:id`. This can be seen at a REPL:
```clojure
=> [(blank-node "the") (blank-node "quick") (blank-node "brown") (blank-node "fox")]
[_:b0 _:b1 _:b2 _:b3]
=> [(blank-node "the") (blank-node "red") (blank-node "fox") (unsafe-blank-node "end")]
[_:b0 _:b4 _:b3 _:end]
```

## Examples
```clojure
(:require '[quoll.rdf as rdf :refer [iri curie lang-literal typed-literal]])

(iri "http://test.com/")
(iri "http://www.w3.org/1999/02/22-rdf-syntax-ns#type" "rdf" "type")
(curie rdf/common-prefixes :rdf/type)
(lang-literal "test" "en")
(typed-literal "literal")
;; The following are equivalent
(typed-literal 5)
(typed-literal "5" (iri "http://www.w3.org/2001/XMLSchema#long" "xsd" "integer"))
(typed-literal "5" (curie rdf/common-prefixes :xsd/integer))
```

## TODO
- Extend to Jena's interfaces.

## License

Copyright © 2023 Paula Gearon

Distributed under the Eclipse Public License version 2.0.

