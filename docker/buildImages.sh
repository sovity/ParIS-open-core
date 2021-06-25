#!/bin/bash

dos2unix open-paris-core/*

# GENERIC IMAGES

mvn -f ../ clean package
cp ../open-paris-core/target/open-paris-core-*.jar open-paris-core/
docker build open-paris-core/ -t registry.gitlab.cc-asp.fraunhofer.de:4567/eis-ids/paris-open/core

#cleanup
rm -rf ../open-paris-core/target
rm -rf ../open-paris-common/target


# fuseki
docker build fuseki/ -t registry.gitlab.cc-asp.fraunhofer.de:4567/eis-ids/paris-open/fuseki

# reverseproxy
docker build reverseproxy/ -t registry.gitlab.cc-asp.fraunhofer.de:4567/eis-ids/paris-open/reverseproxy
