# datomic-graphite-reporter

A Clojure library that reports Datomic transactor metrics to Graphite.


## Usage

See the Datomic documentation on [custom monitoring](http://docs.datomic.com/monitoring.html).

    metrics-callback=datomic-graphite-reporter.core/report-metrics


The library is configured first by system properties,

    graphite.host
    graphite.port
    graphite.prefix

and then falling back to environment variables.

    GRAPHITE_HOST
    GRAPHITE_PORT
    GRAPHITE_PREFIX

Note that there are no defaults for the host or port configuration, but the prefix is optional.


Logging is via slf4j in the "datomic.graphite.reporter" namespace.


## License

Copyright © 2014 Pellucid Analytics

Distributed under the Apache License version 2.0
