#!/usr/bin/bash
cd /app
export CATSPAW_CONFIG="app.properties"
java -cp ./catspaw-listeners.jar com.inyabass.catspaw.listeners.TestExecutor