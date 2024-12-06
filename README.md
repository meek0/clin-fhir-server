# CLIN FHIR Server
This project is a custom fork of the [HAPI-FHIR Starter Project 5.4.0](https://github.com/hapifhir/hapi-fhir-jpaserver-starter/tree/v5.4.0).
It's used to manage FHIR resources for Clin projects.

Authentication is done using Keycloak.

(The main HAPI FHIR project, can be found here: https://github.com/jamesagnew/hapi-fhir)

## Prerequisites

- Oracle Java (JDK) installed: Minimum JDK8 or newer.
- Apache Maven build tool (newest version)
- A PostgreSQL database server
- A Keycloak server
- An Elasticsearch server

The project [clin-localstack](https://github.com/Ferlab-Ste-Justine/clin-localstack) aimed to be used to provide all the required services for this FHIR server local development.

## Configurations

This fork is configured to use PostgreSQL as the database, Keycloak as the authentication server and Elasticsearch as the search engine.
Configurations are done with Spring Boot profiles files, located in `src/main/resources/application-<profile>.yml`.

## Build locally

You can build the project locally using maven (need JDK 11 installed):
```bash
mvn clean install
```
This will create a file called `hapi.war` in your `target` directory. This should be installed in your Web Container according to the instructions for your particular container. For example, if you are using Tomcat, you will want to copy this file to the `webapps/` directory.

An IntelliJ IDEA run configuration is provided in the `.idea`directory: `build.run.xml`.

## Run with docker compose

To deploy with docker compose, you can use the provided Make targets:
- `make start` to start the container (if not existing the image will be created)
- `make stop` to stop the container
- `make build` to force an image build and start the container

The server, will start using the **local** Spring Boot profile, and can be reached at http://localhost:8080.
The compose file is aimed to work with [clin-localstack](https://github.com/Ferlab-Ste-Justine/clin-localstack) started.

The Dockerfile use the Spring Boot profile to configure the server.
Because it's used at build time to create the image, you'll need to rebuild the image if you edited the profile files.

### Debugging
The `docker compose` file is configured to expose the debug port `8000` for the server.
An IntelliJ IDEA debug configuration is provided in the `.idea`directory: `debug_local_stack.run.xml`, to allow debugging.
