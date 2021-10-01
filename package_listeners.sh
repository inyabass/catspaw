#!/usr/bin/env bash
mvn -Plisteners clean compile package
rm -f docker/testexecutor/catspaw-listeners.jar
rm -f docker/testresponder/catspaw-listeners.jar
cp target/catspaw-1.0-jar-with-dependencies.jar docker/testexecutor/catspaw-listeners.jar
cp target/catspaw-1.0-jar-with-dependencies.jar docker/testresponder/catspaw-listeners.jar