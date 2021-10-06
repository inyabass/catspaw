#!/usr/bin/env bash
docker image ls | grep "<none>" | awk '{ print $3 }' | xargs -I {} docker image rm {}