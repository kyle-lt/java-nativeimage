#!/bin/bash

java -agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image -jar target/quarkus-app/quarkus-run.jar

exit 0
