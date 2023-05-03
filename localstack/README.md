# FHIR localstack

# Usage

Run the following command to display usage.

```
sh ./launch.sh [start|status|stop|restart]
```
or
```
chmod +x ./launch.sh
./launch.sh [start|status|stop|restart]
```

# Clean-up

Not part of the script has to be done manually:

```shell
sudo rm -rf data/
```
# Services

Following the list of all services urls:

|Name|URL|
|-|-|
|PostgreSQL|http://localhost:5432|
|Keycloak|http://localhost:8081|
|Elasticsearch|http://localhost:9200|

# Postman

Import `localstack.postman_collection.json` to have a set of pre-built requests. 
