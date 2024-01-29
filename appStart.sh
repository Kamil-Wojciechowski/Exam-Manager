#!/bin/bash

# Assuming the default project name is the directory name where docker-compose.yml resides
project_name=$(basename "$(pwd)")

graylog="${project_name}_graylog_1"
mongo="${project_name}_mongo_1"
elasticsearch="${project_name}_elasticsearch_1"

configure_graylog() {
  # Your curl command to configure Graylog
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

check_and_start_container() {
  local container_name=$1
  container_id=$(docker ps -a -q --no-trunc -f name=^/${container_name}$)

  if [ -n "$container_id" ]; then
    running=$(docker ps -q --no-trunc | grep "$container_id")
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

# Check and start containers
check_and_start_container "$graylog"
check_and_start_container "$mongo"
check_and_start_container "$elasticsearch"

# Check if all containers are running before proceeding
if docker ps | grep -q "$graylog" && docker ps | grep -q "$mongo" && docker ps | grep -q "$elasticsearch"; then
  echo "All required containers are running."
  configure_graylog
else
  echo "One or more required containers are not running. Starting Docker Compose..."
  docker-compose up -d

  # Wait for containers to initialize
  sleep 60

  # Attempt to configure Graylog after waiting
  configure_graylog
fi
