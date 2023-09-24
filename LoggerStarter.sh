#!/bin/bash
graylog=$(docker ps -a -q --no-trunc -f name=exam-logger-graylog-1)
mongo=$(docker ps -a -q --no-trunc -f name=exam-logger-mongo-1)
elasticsearch=$(docker ps -a -q --no-trunc -f name=exam-logger-elasticsearch-1)

if ! [[ -z $graylog || -z $mongo || -z $elasticsearch ]]; then
  if [ -z `docker ps -q --no-trunc | grep $graylog` ]; then
    if [[ -z $graylog ]]; then
      echo "Docker container does not exists"
    else
      docker start $graylog
    fi
  else
    echo "Yes, it's running."
  fi

  if [ -z `docker ps -q --no-trunc | grep $mongo` ]; then
    if [[ -z $mongo ]]; then
      echo "Docker container does not exists"
    else
      docker start $mongo
    fi
  else
    echo "Yes, it's running."
  fi

  if [ -z `docker ps -q --no-trunc | grep $elasticsearch` ]; then
    if [[ -z $elasticsearch ]]; then
      echo "Docker container does not exists"
    else
      docker start $elasticsearch
    fi
  else
    echo "Yes, it's running."
  fi
else
  echo 'Containers does not exists. Starting creation'
  docker-compose -f logger-docker.yml -p exam-logger up -d

  sleep 60

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
fi