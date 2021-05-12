#!/bin/bash

mvn clean package

#java -agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image -jar target/quarkus-app/quarkus-run.jar

mvn package -Pnative -Dquarkus.native.container-build=true -Dquarkus.container-image.build=true

exit 0
