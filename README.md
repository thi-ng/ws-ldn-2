# Clojure/Clojurescript workshop

(WS-LDN-1)

This repo contains a subset of commented examples created during the workshop.

[Workshop notes & references](workshop.org)

## Day 1 namespaces

TBD

## Day 2 namespaces

TBD

## Day 3 namespaces

TBD

## CLJS build w/ advanced optimizations

To build the CLJS examples with advanced optimizations, uncomment the lines with `:optimizations` & `:pretty-print` from the `project.clj` file for the relevant build profile(s). Then compile the source with:

```
lein do clean, cljsbuild once <insert-build-profile-id>
```

## License

Copyright © 2015 Karsten Schmidt

Distributed under the Apache Software License either version 1.0 or (at your option) any later version.
