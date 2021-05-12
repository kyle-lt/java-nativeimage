FROM ghcr.io/graalvm/graalvm-ce:latest

RUN mkdir -p /app
ADD /target/com.ktully.nativeimage.springboot.restservice.restserviceapplication /app
WORKDIR /app

ENTRYPOINT ["./com.ktully.nativeimage.springboot.restservice.restserviceapplication"]
