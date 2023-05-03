#/bin/bash

# declaration of env. variables.
export CLIN_LOCALSTACK_NAME="clin-fhir-localstack"
export DATA_DIRECTORY="$(pwd)/data";
export POSTGRES_DATA_DIRECTORY="$DATA_DIRECTORY/postgres"
export KEYCLOAK_DATA_DIRECTORY="$DATA_DIRECTORY/keycloak"
export ELASTICSEARCH_DATA_DIRECTORY="$DATA_DIRECTORY/elasticsearch"
export INIT_DATA_DIRECTORY="$(pwd)/init";

SERVICES="clin-fhir-postgres clin-fhir-keycloak clin-fhir-elasticsearch"

showUsage()
{
  echo "All-in-one FHIR localstack managment"
  echo
  echo "Usage:"
  echo "  sh ./launch.sh [COMMAND]"
  echo "  ./launch.sh [COMMAND] (requires chmod +x ./launch.sh)"
  echo
  echo "Commands:"
  echo "  start         Start all docker images (also performs init)"
  echo "  status        Show current CLIN dockers list and status"
  echo "  stop          Stop all CLIN docker containers"
  echo "  restart       Equivalent to stop followed by start"
  echo
}

init()
{
  [ -d $ELASTICSEARCH_DATA_DIRECTORY ] || mkdir -p $ELASTICSEARCH_DATA_DIRECTORY/nodes/0/indices
  [ -d $POSTGRES_DATA_DIRECTORY ] || mkdir -p $POSTGRES_DATA_DIRECTORY
}

start()
{
  init
  echo "Start docker containers..."
  docker-compose -p $CLIN_LOCALSTACK_NAME up -d --remove-orphans $SERVICES
}

status()
{
  docker ps --format "table {{.ID}}\t{{.Names}}\t{{.Status}}" | grep $CLIN_LOCALSTACK_NAME
}

stop()
{
  echo "Stop docker containers..."
  docker-compose -p $CLIN_LOCALSTACK_NAME down
}

restart()
{
  echo "Restarting..."
  stop
  start
}

# main part of the script
case "$1" in
  "start")
  start
  ;;
  "status")
  status
  ;;
  "stop")
  stop
  ;;
  "restart")
  restart
  ;;
  *)
  showUsage
  ;;
esac
