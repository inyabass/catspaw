#!/usr/bin/env bash
cd ../..
./package_listeners.sh
./cleanupimages.sh
cd docker/testresponder
docker build -t catsresponder:latest .