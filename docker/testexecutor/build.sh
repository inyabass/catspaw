#!/usr/bin/env bash
cd ../..
./package_listeners.sh
./cleanupimages.sh
cd docker/testexecutor
dos2unix app.properties
dos2unix runapp.sh
docker build -t catsexecutor:latest .