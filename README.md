# RuDolF

RDF datatypes.

## Usage
### Leiningen/Boot
```clojure
[org.clojars.quoll/rudolf "0.0.1"]
```

### Clojure CLI/deps.edn
```clojure
org.clojars.quoll/rudolf {:mvn/version "0.0.1"}
```

The namespace contains basic RDF datatypes, for portable use in other applications.

```clojure
(:require '[quoll.rdf.rudolf as rdf])

(iri "http://test.com/")
(iri "http://www.w3.org/1999/02/22-rdf-syntax-ns#type" "rdf" "type")
(typed-literal "literal")
(typed-literal "5" (iri "http://www.w3.org/2001/XMLSchema#long" "xsd" "long"))
(lang-literal "test" "en")
```

## License

Copyright © 2023 Paula Gearon

Distributed under the Eclipse Public License version 2.0.

