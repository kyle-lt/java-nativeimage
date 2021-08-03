#!/bin/bash

# Testing OTel SDK Extension AutoConfigure using env vars
export OTEL_RESOURCE_ATTRIBUTES=service.name=java-nativeimage-quarkus-restservice,service.namespace=kjt-java-nativeimage
export OTEL_TRACES_EXPORTER=otlp
export OTEL_EXPORTER_OTLP_ENDPOINT=http://host.docker.internal:4317

# Run the jar
java -jar target/quarkus-app/quarkus*jar

exit 0
