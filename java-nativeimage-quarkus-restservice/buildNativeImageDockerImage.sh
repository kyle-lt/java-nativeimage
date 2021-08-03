#!/bin/bash

mvn clean

mvn package -Pnative -Dquarkus.native.container-build=true -Dquarkus.container-image.build=true

exit 0
