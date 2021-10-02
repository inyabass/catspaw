#!/usr/bin/env bash
cd ../..
./package_api.sh
./cleanupimages.sh
cd docker/api
docker build -t catsapi:latest .