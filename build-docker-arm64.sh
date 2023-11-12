#!/bin/bash
./build.sh

docker build --platform linux/arm64 --no-cache -f src/main/docker/Dockerfile.jvm  -t  nishitproject/backend:latest .

#clean up
image_ids=$(docker images | grep dateq | grep none)
if [ "${image_ids:-0}" == 0 ]; then
  echo 'Skip clean up'
else
  docker images | grep dateq | grep none | awk '{print $3}' | xargs docker rmi
fi

if [ ! -z $1 ]; then
  echo "Tagging latest as nishitproject/backend:$1"
  docker tag nishitproject/backend:latest nishitproject/backend:$1
fi
