#!/bin/bash
./build.sh
PLATFORM=${1:-"linux/amd64"}
docker build --no-cache --platform "$PLATFORM" -f src/main/docker/Dockerfile.jvm  -t  nishitproject/backend:backend-dev .
