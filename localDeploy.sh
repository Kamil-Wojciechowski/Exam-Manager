#!/bin/bash
prefix="exam-manager-"

spring_boot_service="springbootapp"
graylog_service="graylog"
mongo_service="mongo"
elasticsearch_service="elasticsearch"
postgresql_service="postgresql"

check_and_start_container() {
  local container_name=$1
  container_id=$(docker ps -a -q --no-trunc --filter "name=${prefix}${container_name}")

  if [ -n "$container_id" ]; then
    running=$(docker ps -q --no-trunc --filter "id=${container_id}")
    if [ -z "$running" ]; then
      echo "Starting $container_name..."
      docker start "$container_id"
    else
      echo "$container_name is already running."
    fi
  else
    echo "$container_name container does not exist."
  fi
}

configure_graylog() {
  echo "Configuring Graylog..."
  curl --location 'http://127.0.0.1:9000/api/system/inputs' \
    --header 'X-Requested-By: Local' \
    --header 'Content-Type: application/json' \
    --header 'Authorization: Basic YWRtaW46YWRtaW4=' \
    --data '{
        "title": "UDP Input",
        "type": "org.graylog2.inputs.gelf.udp.GELFUDPInput",
        "configuration": {
            "bind_address": "0.0.0.0",
            "port": 12201,
            "recv_buffer_size": 262144,
            "number_worker_threads": 12,
            "override_source": null,
            "decompress_size_limit": 8388608
        },
        "global": true
    }'
}

containers_exist_and_running=true

check_and_start_container "$spring_boot_service"
check_and_start_container "$graylog_service"
check_and_start_container "$mongo_service"
check_and_start_container "$elasticsearch_service"
check_and_start_container "$postgresql_service"

if ! docker ps -a --format '{{.Names}}' | grep -q "$spring_boot_service" || ! docker ps -a --format '{{.Names}}' | grep -q "$graylog_service" || ! docker ps -a --format '{{.Names}}' | grep -q "$mongo_service" || ! docker ps -a --format '{{.Names}}' | grep -q "$elasticsearch_service" || ! docker ps -a --format '{{.Names}}' | grep -q "$postgresql_service"; then
  containers_exist_and_running=false
fi

if [ "$containers_exist_and_running" = false ]; then
  echo "One or more required containers do not exist. Starting all services with Docker Compose..."
  docker-compose up -d

  sleep 60

  configure_graylog
else
  echo "All required containers are up and running. No further action is taken."
fi
