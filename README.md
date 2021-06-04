# Open-Core IDS Participant Information Service

This is an implementation of an International Data Spaces (IDS) Participant Information Service (ParIS), which is a 
registry for IDS Participant self-description documents. It is currently under development and intends to act as a
reference for members of the International Data Spaces Association (IDSA) to help with the implementation of custom ParIS
solutions. Work on this repository closely aligns with the IDS Handshake Document, which describes the concepts of
communication on the IDS in textual form.

General information about the necessity of the ParIS, the technical workflow and IDS interactions with it, as well as
details about the data model used, can be found in the [IDS-G](https://github.com/International-Data-Spaces-Association/IDS-G/tree/feature/paris/core/ParIS#data-model-participant-and-participant-attributes).

## Purpose

The goal of this implementation is to show how the concepts introduced in the Handshake Document can be turned into an
actual application. It, therefore, demonstrates the usage of the IDS Information Model for core communication tasks.
More specifically, it shows:
- Implementation of the messaging interfaces for IDS infrastructure-level communication,
- Information flow of typical interactions with the ParIS

Security is currently supported in terms of TLS via a reverse proxy.

## Repository Structure

[open-paris-core](open-paris-core): The main Maven Artifact to start with.

[open-paris-common](open-paris-common): Core functionality specific to ParIS. Note that core functionality, which is more general
to any IDS indexing service, can be found in the open-index-common module of the [IDS Metadata Broker](https://github.com/International-Data-Spaces-Association/metadata-broker-open-core).

[docker](docker): Docker and DockerCompose files to deploy the IDS ParIS.

## Running the ParIS

The steps for bringing up a ParIS instance depend on the host where the ParIS should be deployed. The easiest option is
to run the instance on localhost, which is described in the following. We assume that the Docker command-line tools are
installed on your system.

Build the Docker Images, Prepare and Check the Docker Compose File:

1. __Prepare the SSL certificate:__ On your host system, create a directory /etc/idscert/localhost (Linux),
   C:\etc\idscert\localhost (Windows) and put two files into this directory:
    * server.crt: an x509 certificate, either self-signed or from an official CA
    * server.key: the private key for the certificate

   In the case that you received Keys from our partner Institutes or us, that are stored in a .pem format a conversion to .crt and .key is required for the usage in the reverse proxy.

       openssl x509 -in example_cert.pem -out server.crt openssl rsa -in example_key.pem -out server.key mkdir cert mv server.crt cert/ mv server.key cert/

2. __Build the Docker Images, Prepare and Check the Docker Compose File__: Next, you can either build your customized docker containers as demonstrated in 2.1, or make use of docker images provided by us, see 2.2.

   2.1 If you want to use a docker-compose file that uses locally built images, please execute following steps:
    - You can find a build script for the images in the docker directory: docker/buildImages.sh .
    - Note that you need to have Maven installed for executing the script.
      Make sure **Java 11** and **Maven 3.6.3** or later are installed in your local environment to build the docker image.

   2.2
   The ```docker-compose pull``` command can be used to download or update images provided by us reflecting the current
   state of this repository. If no local images are present, Docker will try this automatically on the first start,
   though it will not automatically update on subsequent starts. Note that this command needs to be executed in the same
   directory as the docker-compose.yml file, see step 3.


3. __Run the services__: We provide a [docker-compose file for a localhost setup](docker/composefiles/paris-localhost/docker-compose.yml).
   Download the file, change
   to the directory where it is located and run ```docker-compose up```.
   
4. __Interacting with the ParIS__: We maintain Postman collections illustrating interactions with the Broker and the ParIS.
[This collection](https://www.getpostman.com/collections/d1e80e1f369ae2a8f492) illustrates IDS-REST interactions.
   
## Built With

* [Maven](https://maven.apache.org/) - Dependency Management
* [Spring Boot](https://projects.spring.io/spring-boot/) - Application Framework
* [Apache Jena](https://jena.apache.org/documentation/) - Parsing and serializing RDF and Fuseki as triple store for meta data

## Developers

The core development and architecture is driven by
- [Matthias Böckmann](https://github.com/maboeckmann), [Fraunhofer IAIS](https://www.iais.fraunhofer.de/)
- [Stefanie Koslowski](https://github.com/stefkoslowski), [Fraunhofer IAIS](https://www.iais.fraunhofer.de/)

with significant contributions, comments, and support by
- [Sebastian Bader](https://github.com/sebbader/), [Fraunhofer IAIS](https://www.iais.fraunhofer.de/)

## Contact

[contact@ids.fraunhofer.de](mailto:contact@ids.fraunhofer.de)