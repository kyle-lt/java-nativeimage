FROM ghcr.io/graalvm/graalvm-ce:latest

RUN mkdir -p /app
#ADD /target/quarkus-app /app
ADD target/java-nativeimage-quarkus-restservice-1.0.0-SNAPSHOT-runner /app
WORKDIR /app

#ENTRYPOINT ["java","-jar","quarkus-run.jar"]
ENTRYPOINT ["./java-nativeimage-quarkus-restservice-1.0.0-SNAPSHOT-runner"]
