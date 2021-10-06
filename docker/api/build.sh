#!/usr/bin/env bash
cd ../..
./package_api.sh
./cleanupimages.sh
cd docker/api
dos2unix app.properties
dos2unix runapp.sh
docker build -t catsapi:latest .