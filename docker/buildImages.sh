#!/bin/bash

dos2unix participant-information-service-open-core/*

# GENERIC IMAGES

mvn -f ../ clean package
cp ../open-paris-core/target/participant-information-service-open-core-*.jar participant-information-service-open-core/
docker build participant-information-service-open-core/ -t registry.gitlab.cc-asp.fraunhofer.de:4567/eis-ids/paris-open/core

#cleanup
rm -rf ../open-paris-core/target
rm -rf ../open-paris-common/target


# fuseki
docker build fuseki/ -t registry.gitlab.cc-asp.fraunhofer.de:4567/eis-ids/paris-open/fuseki

# reverseproxy
docker build reverseproxy/ -t registry.gitlab.cc-asp.fraunhofer.de:4567/eis-ids/paris-open/reverseproxy
