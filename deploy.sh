./build-docker.sh
docker push nishitproject/backend:backend-dev
ssh nish "./deploy.sh"
