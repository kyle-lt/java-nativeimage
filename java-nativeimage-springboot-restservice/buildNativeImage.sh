#!/bin/bash

#echo "Ensuring that we are using the GraalVM with Native Image support."
#sdk use java 21.0.0.2.r8-grl
#echo "Done!"

echo "Building executable jar."
mvn package
echo "Done!"

#echo "Attempting to detect dynamic features and generate configuration files in src/main/resources/META-INF/native-image"
#java -agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image -jar target/restservice-0.0.1-SNAPSHOT-exec.jar &
#PID=$!
#echo "Waiting 15 seconds to gather data and generate configuration files..."
#sleep 15
#echo "Ok, long enough, let's move on."
#kill $PID
#echo "Done!"

echo "Starting maven native image build."
mvn -Pnative-image package
echo "Done!"

echo "Java Native Image available at target/com.ktully.nativeimage.springboot.restservice.restserviceapplication"

exit 0
