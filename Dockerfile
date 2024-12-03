FROM maven:3.6.3-jdk-11-slim AS build-hapi

WORKDIR /tmp/clin-fhir-server
COPY . .

RUN mvn clean install -DskipTests

FROM tomcat:9-jre11
RUN rm -rf /usr/local/tomcat/webapps/*
RUN mkdir -p /data/hapi/lucenefiles && chmod 775 /data/hapi/lucenefiles
COPY --from=build-hapi /tmp/clin-fhir-server/target/hapi.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080

CMD ["catalina.sh", "run"]
