#!/bin/bash

#name=$(docker ps -a --format "table {{.Names}}" | grep $1)
#echo "logging ${name}"
docker-compose logs -f $1
