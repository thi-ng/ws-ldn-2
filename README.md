# Clojure/Clojurescript workshop

(WS-LDN-2)

This repo contains a subset of commented examples created during the workshop.

[Workshop notes & references](workshop.org)

# Example app

We used geographic data from the
[ONS linked data portal](http://statistics.data.gov.uk/) and sampled &
converted housing datasets from the
[London Data Store](http://data.london.gov.uk/dataset/average-house-prices-borough)
to RDF in order to build heatmap visualizations of the London housing
situation:

This heatmap shows average property sale prices per borough in 2013/14:

![London house prices 2013/14](assets/ldn-heatmap.jpg)

This heatmap is based on the number of property sales, showing a clear bias in the south east:

![London house sales (count) 2013/14](assets/ldn-heatmap-count.jpg)

## Overview

- Brief review of Clojure concepts
- Processing data in parallel (reducers, fold) and/or concurrently (promises, delays, agents, futures, agents)
- Organizing code around [core.async](https://github.com/clojure/core.async) channels
- Brief graph theory & representations in code form
- Introduction to [thi.ng/fabric](thi.ng/fabric) compute graph
- Using graphs as caching network to compute data efficiently
- Linked Data (LD) introduction & common vocabularies
- Mapping CSV data to graphs using LD vocabularies
- Defining macros to simplify boilerplate & create a DSL
- Importing & querying Linked Data sets/graphs using a DSL
- Using Graphviz to debug queries & LD datasets
- Introduction to component driven workflow
- Setting up a simple LD server (using [components](https://github.com/stuartsierra/component))
- Building a UI with [Figwheel](https://github.com/bhauman/lein-figwheel), [Reagent](http://reagent-project.github.io/) (and [React.js](http://facebook.github.io/react/))
- Representing & transforming DOM fragments in Clojure(script)
- Event handling & event busses using core.async
- Routing UI state changes to view components, adding responsive features
- WebGL introduction, Clojurescript examples
- [Visualizing data in SVG](http://thi.ng/geom) & WebGL
- Composing WebGL shaders from [re-usable fragments](http://thi.ng/shadergraph)

## Namespaces

TBD

## CLJS build w/ advanced optimizations

To build the CLJS examples with advanced optimizations, uncomment the lines with `:optimizations` & `:pretty-print` from the `project.clj` file for the relevant build profile(s). Then compile the source with:

```
lein do clean, cljsbuild once <insert-build-profile-id>
```

## License

Copyright © 2015 Karsten Schmidt

Distributed under the Apache Software License either version 1.0 or (at your option) any later version.
