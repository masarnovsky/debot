#!/bin/bash

# chmod u+x redeploy.sh to make file executable

echo "pull last version from github"
git pull origin master
echo "pulled"

echo "stopping debot container"
docker stop debot
echo "debot container was stopped"

echo "removing debot container"
docker rm debot
echo "debot container was removed"

echo "removing debot image"
docker image rm debot
echo "debot image was removed"

echo "building new debot image"
docker build -t debot .
echo "debot image was build"

echo "running debot and postgres from docker-compose"
docker-compose up -d