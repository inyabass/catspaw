#!/usr/bin/env bash
mvn -Plisteners clean compile package
rm -f latestjars/catspaw-listeners.jar
cp target/catspaw-1.0-jar-with-dependencies.jar latestjars/catspaw-listeners.jar