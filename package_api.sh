#!/usr/bin/env bash
mvn -Pspring clean package spring-boot:repackage
rm -f latestjars/catspaw-api.jar
cp target/catspaw-1.0.jar latestjars/catspaw-api.jar