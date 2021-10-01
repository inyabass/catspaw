#!/usr/bin/env bash
mvn -Pspring clean package spring-boot:repackage
rm -f docker/api/catspaw-api.jar
cp target/catspaw-1.0.jar docker/api/catspaw-api.jar