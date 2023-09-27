# RuDolF

RDF datatypes.

## Usage
### Leiningen/Boot
```clojure
[org.clojars.quoll/rudolf "0.1.1"]
```

### Clojure CLI/deps.edn
```clojure
org.clojars.quoll/rudolf {:mvn/version "0.1.1"}
```

The namespace contains basic RDF datatypes, for portable use in other applications.

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

## License

Copyright © 2023 Paula Gearon

Distributed under the Eclipse Public License version 2.0.

