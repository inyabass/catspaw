#!/usr/bin/env bash
#
# Run the API App inside the container
#
cd /app
export CATSPAW_CONFIG="app.properties"
java -jar catspaw-api.jar