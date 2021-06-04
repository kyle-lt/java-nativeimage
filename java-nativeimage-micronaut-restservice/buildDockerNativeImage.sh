#!/bin/bash

mvn clean package -Pgraalvm -Dpackaging=docker-native

exit 0
