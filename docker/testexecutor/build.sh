#!/usr/bin/env bash
cd ../..
./package_listeners.sh
./cleanupimages.sh
cd docker/testexecutor
docker build -t catsexecutor:latest .