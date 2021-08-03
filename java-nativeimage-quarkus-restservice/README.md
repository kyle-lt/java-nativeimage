# java-nativeimage-quarkus-restservice project

This project makes use of the [OTel SDK Extension AutoConfigure](https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure) module/package.  

The reason for using the AutoConfigure package is to take advantage of the convenient ability to completely configure the OTel SDK via environment variables.

In order to do so, it also uses a "custom" `Main` method (as outlined [here](https://quarkus.io/guides/lifecycle)) to bootstrap the GlobalOpenTelemetry SDK.

It does  __not__  use the built-in quarkus OTel support (as in this [example](https://quarkus.io/guides/opentelemetry)).
- I have a separate repo [here](http://todo) (//TODO - create and link) that uses the built-in method

## Run Code in JVM, Dev Mode

From the base directory, run the following command:

```bash
mvn quarkus:dev
```

## Build a Runnable JAR

From the base directory, run the following command:

```bash
mvn clean package
```

## Run the JAR

Normally, you could just run something like:

```bash
# run with java command
java -jar target/quarkus-app/quarkus-run.jar
```

But, since we are configuring the SDK with env vars, those need to be set before running.

So, there is a helper script that accomplishes that named `jar-run.sh`.

Edit the environment variables in the script to fit your use case, e.g.:

```bash
export OTEL_RESOURCE_ATTRIBUTES=service.name=java-nativeimage-quarkus-restservice,service.namespace=kjt-java-nativeimage
export OTEL_TRACES_EXPORTER=otlp
export OTEL_EXPORTER_OTLP_ENDPOINT=http://host.docker.internal:4317
```

Then run the script:

```bash
./jar-run.sh
```

## Build Native Image and Docker Image

There is a helper shell script that does this piece named `buildNativeImageDockerImage.sh`.  Just run it:

> __NOTE:__  If running this on your laptop, it is highly recommended to give Docker lots of memory, 8GB seems to work fine on my MacBook Pro

```bash
./buildNativeImageDockerImage.sh
```

It'll create an image named `docker.io/kjtully/java-nativeimage-quarkus-restservice:latest`
- If you want to change that, edit the `application.properties` file

```bash
# Docker Image Build Configs
quarkus.container-image.group=kjtully
quarkus.container-image.name=java-nativeimage-quarkus-restservice
quarkus.container-image.tag=latest
quarkus.container-image.builder=docker
```

## Run the Docker Image with Docker-Compose


