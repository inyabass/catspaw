#!/usr/bin/env bash
export CATSPAW_CONFIG="src/main/resources/com/inyabass/catspaw/TestRequestListener.properties"
mvn compile exec:exec -PTestExecutor